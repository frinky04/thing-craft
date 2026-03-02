/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package paulscode.sound.codecs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import paulscode.sound.ICodec;
import paulscode.sound.SoundBuffer;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemLogger;

@Environment(value=EnvType.CLIENT)
public class CodecWav
implements ICodec {
    private static final boolean GET = false;
    private static final boolean SET = true;
    private static final boolean XXX = false;
    private boolean endOfStream = false;
    private boolean initialized = false;
    private AudioInputStream myAudioInputStream = null;
    private SoundSystemLogger logger = SoundSystemConfig.getLogger();

    public void reverseByteOrder(boolean bl) {
    }

    public boolean initialize(URL uRL) {
        this.initialized(true, false);
        this.cleanup();
        if (uRL == null) {
            this.errorMessage("url null in method 'initialize'");
            this.cleanup();
            return false;
        }
        try {
            this.myAudioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(uRL.openStream()));
        }
        catch (UnsupportedAudioFileException unsupportedAudioFileException) {
            this.errorMessage("Unsupported audio format in method 'initialize'");
            this.printStackTrace(unsupportedAudioFileException);
            return false;
        }
        catch (IOException iOException) {
            this.errorMessage("Error setting up audio input stream in method 'initialize'");
            this.printStackTrace(iOException);
            return false;
        }
        this.endOfStream(true, false);
        this.initialized(true, true);
        return true;
    }

    public boolean initialized() {
        return this.initialized(false, false);
    }

    public SoundBuffer read() {
        int i;
        if (this.myAudioInputStream == null) {
            return null;
        }
        AudioFormat audioFormat = this.myAudioInputStream.getFormat();
        if (audioFormat == null) {
            this.errorMessage("Audio Format null in method 'read'");
            return null;
        }
        int j = 0;
        byte[] bs = new byte[SoundSystemConfig.getStreamingBufferSize()];
        try {
            for (i = 0; !this.endOfStream(false, false) && i < bs.length; i += j) {
                j = this.myAudioInputStream.read(bs, i, bs.length - i);
                if (j > 0) continue;
                this.endOfStream(true, true);
                break;
            }
        }
        catch (IOException iOException) {
            this.endOfStream(true, true);
            return null;
        }
        if (i <= 0) {
            return null;
        }
        if (i < bs.length) {
            bs = CodecWav.trimArray(bs, i);
        }
        byte[] cs = CodecWav.convertAudioBytes(bs, audioFormat.getSampleSizeInBits() == 16);
        SoundBuffer soundBuffer = new SoundBuffer(cs, audioFormat);
        return soundBuffer;
    }

    /*
     * WARNING - void declaration
     */
    public SoundBuffer readAll() {
        byte[] cs;
        void k;
        int l;
        int j;
        if (this.myAudioInputStream == null) {
            this.errorMessage("Audio input stream null in method 'readAll'");
            return null;
        }
        AudioFormat audioFormat = this.myAudioInputStream.getFormat();
        if (audioFormat == null) {
            this.errorMessage("Audio Format null in method 'readAll'");
            return null;
        }
        Object object = null;
        int i = audioFormat.getChannels() * (int)this.myAudioInputStream.getFrameLength() * audioFormat.getSampleSizeInBits() / 8;
        if (i > 0) {
            byte[] bs = new byte[audioFormat.getChannels() * (int)this.myAudioInputStream.getFrameLength() * audioFormat.getSampleSizeInBits() / 8];
            boolean bl = false;
            try {
                for (l = 0; (j = this.myAudioInputStream.read(bs, l, bs.length - l)) != -1 && l < bs.length; l += j) {
                }
            }
            catch (IOException iOException) {
                this.errorMessage("Exception thrown while reading from the AudioInputStream (location #1).");
                this.printStackTrace(iOException);
                return null;
            }
        }
        j = 0;
        l = 0;
        int n = 0;
        Object object3 = null;
        byte[] es = new byte[SoundSystemConfig.getFileChunkSize()];
        while (!this.endOfStream(false, false) && k < SoundSystemConfig.getMaxFileSize()) {
            int m;
            n = 0;
            try {
                for (m = 0; m < es.length; m += n) {
                    n = this.myAudioInputStream.read(es, m, es.length - m);
                    if (n > 0) continue;
                    this.endOfStream(true, true);
                    break;
                }
            }
            catch (IOException iOException2) {
                this.errorMessage("Exception thrown while reading from the AudioInputStream (location #2).");
                this.printStackTrace(iOException2);
                return null;
            }
            k += m;
            cs = CodecWav.appendByteArrays(cs, es, m);
        }
        byte[] ds = CodecWav.convertAudioBytes(cs, audioFormat.getSampleSizeInBits() == 16);
        SoundBuffer soundBuffer = new SoundBuffer(ds, audioFormat);
        try {
            this.myAudioInputStream.close();
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return soundBuffer;
    }

    public boolean endOfStream() {
        return this.endOfStream(false, false);
    }

    public void cleanup() {
        if (this.myAudioInputStream != null) {
            try {
                this.myAudioInputStream.close();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.myAudioInputStream = null;
    }

    public AudioFormat getAudioFormat() {
        if (this.myAudioInputStream == null) {
            return null;
        }
        return this.myAudioInputStream.getFormat();
    }

    private synchronized boolean initialized(boolean bl, boolean bl2) {
        if (bl) {
            this.initialized = bl2;
        }
        return this.initialized;
    }

    private synchronized boolean endOfStream(boolean bl, boolean bl2) {
        if (bl) {
            this.endOfStream = bl2;
        }
        return this.endOfStream;
    }

    private static byte[] trimArray(byte[] bs, int i) {
        byte[] cs;
        Object object = null;
        if (bs != null && bs.length > i) {
            cs = new byte[i];
            System.arraycopy(bs, 0, cs, 0, i);
        }
        return cs;
    }

    private static byte[] convertAudioBytes(byte[] bs, boolean bl) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bs.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        ByteBuffer byteBuffer2 = ByteBuffer.wrap(bs);
        byteBuffer2.order(ByteOrder.LITTLE_ENDIAN);
        if (bl) {
            ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
            ShortBuffer shortBuffer2 = byteBuffer2.asShortBuffer();
            while (shortBuffer2.hasRemaining()) {
                shortBuffer.put(shortBuffer2.get());
            }
        } else {
            while (byteBuffer2.hasRemaining()) {
                byteBuffer.put(byteBuffer2.get());
            }
        }
        byteBuffer.rewind();
        if (!byteBuffer.hasArray()) {
            byte[] cs = new byte[byteBuffer.capacity()];
            byteBuffer.get(cs);
            byteBuffer.clear();
            return cs;
        }
        return byteBuffer.array();
    }

    private static byte[] appendByteArrays(byte[] bs, byte[] cs, int i) {
        byte[] fs;
        Object var1_1;
        if (bs == null && cs == null) {
            return null;
        }
        if (bs == null) {
            byte[] ds = new byte[i];
            System.arraycopy(cs, 0, ds, 0, i);
            cs = null;
        } else if (var1_1 == null) {
            byte[] es = new byte[bs.length];
            System.arraycopy(bs, 0, es, 0, bs.length);
            bs = null;
        } else {
            byte[] byArray;
            fs = new byte[byArray.length + i];
            System.arraycopy(byArray, 0, fs, 0, byArray.length);
            System.arraycopy(var1_1, 0, fs, byArray.length, i);
            byArray = null;
            var1_1 = null;
        }
        return fs;
    }

    private void errorMessage(String string) {
        this.logger.errorMessage("CodecWav", string, 0);
    }

    private void printStackTrace(Exception exception) {
        this.logger.printStackTrace(exception, 1);
    }
}

