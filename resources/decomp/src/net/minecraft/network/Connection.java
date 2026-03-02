/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class Connection {
    public static final Object LOCK = new Object();
    public static int READ_THREAD_COUNTER;
    public static int WRITE_THREAD_COUNTER;
    private Object lock = new Object();
    private Socket socket;
    private final SocketAddress address;
    private DataInputStream input;
    private DataOutputStream output;
    private boolean open = true;
    private List readQueue = Collections.synchronizedList(new ArrayList());
    private List sendQueue = Collections.synchronizedList(new ArrayList());
    private List delayedSendQueue = Collections.synchronizedList(new ArrayList());
    private PacketHandler listener;
    private boolean closed = false;
    private Thread writer;
    private Thread reader;
    private boolean disconnected = false;
    private String disconnectReason = "";
    private int timeout = 0;
    private int sendQueueSize = 0;
    private int delay = 0;

    public Connection(Socket socket, String name, PacketHandler listener) {
        this.socket = socket;
        this.address = socket.getRemoteSocketAddress();
        this.listener = listener;
        socket.setTrafficClass(24);
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
        this.reader = new Thread(name + " read thread"){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            public void run() {
                Object object = LOCK;
                synchronized (object) {
                    ++READ_THREAD_COUNTER;
                }
                try {
                    while (Connection.this.open && !Connection.this.closed) {
                        Connection.this.read();
                    }
                }
                finally {
                    object = LOCK;
                    synchronized (object) {
                        --READ_THREAD_COUNTER;
                    }
                }
            }
        };
        this.writer = new Thread(name + " write thread"){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            public void run() {
                Object object = LOCK;
                synchronized (object) {
                    ++WRITE_THREAD_COUNTER;
                }
                try {
                    while (Connection.this.open) {
                        Connection.this.write();
                    }
                }
                finally {
                    object = LOCK;
                    synchronized (object) {
                        --WRITE_THREAD_COUNTER;
                    }
                }
            }
        };
        this.reader.start();
        this.writer.start();
    }

    @Environment(value=EnvType.SERVER)
    public void setListener(PacketHandler listener) {
        this.listener = listener;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void send(Packet packet) {
        if (this.closed) {
            return;
        }
        Object object = this.lock;
        synchronized (object) {
            this.sendQueueSize += packet.getSize() + 1;
            if (packet.shouldDelay) {
                this.delayedSendQueue.add(packet);
            } else {
                this.sendQueue.add(packet);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    private void write() {
        block12: {
            try {
                boolean i = true;
                if (!this.sendQueue.isEmpty()) {
                    void packet2;
                    i = false;
                    Object object = this.lock;
                    synchronized (object) {
                        try {
                            Packet packet = (Packet)this.sendQueue.remove(0);
                            this.sendQueueSize -= packet.getSize() + 1;
                        }
                        catch (Throwable throwable) {
                            void throwable2;
                            throw throwable2;
                        }
                    }
                    Packet.serialize((Packet)packet2, this.output);
                }
                if ((i || this.delay-- <= 0) && !this.delayedSendQueue.isEmpty()) {
                    void packet4;
                    i = false;
                    Object object2 = this.lock;
                    synchronized (object2) {
                        try {
                            Packet packet3 = (Packet)this.delayedSendQueue.remove(0);
                            this.sendQueueSize -= packet3.getSize() + 1;
                        }
                        catch (Throwable throwable) {
                            void throwable2;
                            throw throwable2;
                        }
                    }
                    Packet.serialize((Packet)packet4, this.output);
                    this.delay = 50;
                }
                if (i) {
                    Thread.sleep(10L);
                }
            }
            catch (InterruptedException interruptedException) {
            }
            catch (Exception exception) {
                if (this.disconnected) break block12;
                this.handleException(exception);
            }
        }
    }

    private void read() {
        block4: {
            try {
                Packet packet = Packet.deserialize(this.input);
                if (packet != null) {
                    this.readQueue.add(packet);
                } else {
                    this.close("End of stream");
                }
            }
            catch (Exception exception) {
                if (this.disconnected) break block4;
                this.handleException(exception);
            }
        }
    }

    private void handleException(Exception exception) {
        exception.printStackTrace();
        this.close("Internal exception: " + exception.toString());
    }

    public void close(String reason) {
        if (!this.open) {
            return;
        }
        this.disconnected = true;
        this.disconnectReason = reason;
        new Thread(){

            public void run() {
                try {
                    Thread.sleep(5000L);
                    if (Connection.this.reader.isAlive()) {
                        try {
                            Connection.this.reader.stop();
                        }
                        catch (Throwable throwable) {
                            // empty catch block
                        }
                    }
                    if (Connection.this.writer.isAlive()) {
                        try {
                            Connection.this.writer.stop();
                        }
                        catch (Throwable throwable) {}
                    }
                }
                catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }.start();
        this.open = false;
        try {
            this.input.close();
            this.input = null;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            this.output.close();
            this.output = null;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            this.socket.close();
            this.socket = null;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void tick() {
        if (this.sendQueueSize > 0x100000) {
            this.close("Send buffer overflow");
        }
        if (this.readQueue.isEmpty()) {
            if (this.timeout++ == 1200) {
                this.close("Timed out");
            }
        } else {
            this.timeout = 0;
        }
        int i = 100;
        while (!this.readQueue.isEmpty() && i-- >= 0) {
            Packet packet = (Packet)this.readQueue.remove(0);
            packet.handle(this.listener);
        }
        if (this.disconnected && this.readQueue.isEmpty()) {
            this.listener.onDisconnect(this.disconnectReason);
        }
    }

    @Environment(value=EnvType.SERVER)
    public SocketAddress getAddress() {
        return this.address;
    }

    @Environment(value=EnvType.SERVER)
    public void close() {
        this.closed = true;
        this.reader.interrupt();
        new Thread(){

            public void run() {
                try {
                    Thread.sleep(2000L);
                    if (Connection.this.open) {
                        Connection.this.writer.interrupt();
                        Connection.this.close("Connection closed");
                    }
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }.start();
    }

    @Environment(value=EnvType.SERVER)
    public int getDelayedSendQueueSize() {
        return this.delayedSendQueue.size();
    }
}

