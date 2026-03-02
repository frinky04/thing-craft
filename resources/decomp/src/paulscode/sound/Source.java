/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package paulscode.sound;

import java.net.URL;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import paulscode.sound.Channel;
import paulscode.sound.FilenameURL;
import paulscode.sound.ICodec;
import paulscode.sound.Library;
import paulscode.sound.SoundBuffer;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemLogger;
import paulscode.sound.Vector3D;

@Environment(value=EnvType.CLIENT)
public class Source {
    protected Class libraryType = Library.class;
    private static final boolean GET = false;
    private static final boolean SET = true;
    private static final boolean XXX = false;
    private SoundSystemLogger logger;
    public boolean rawDataStream = false;
    public AudioFormat rawDataFormat = null;
    public boolean temporary = false;
    public boolean priority = false;
    public boolean toStream = false;
    public boolean toLoop = false;
    public boolean toPlay = false;
    public String sourcename = "";
    public FilenameURL filenameURL = null;
    public Vector3D position;
    public int attModel = 0;
    public float distOrRoll = 0.0f;
    public float gain = 1.0f;
    public float sourceVolume = 1.0f;
    protected float pitch = 1.0f;
    public float distanceFromListener = 0.0f;
    public Channel channel = null;
    private boolean active = true;
    private boolean stopped = true;
    private boolean paused = false;
    protected SoundBuffer soundBuffer = null;
    protected ICodec codec = null;
    protected boolean reverseByteOrder = false;
    protected LinkedList soundSequenceQueue = null;
    protected final Object soundSequenceLock = new Object();
    public boolean preLoad = false;
    protected float fadeOutGain = -1.0f;
    protected float fadeInGain = 1.0f;
    protected long fadeOutMilis = 0L;
    protected long fadeInMilis = 0L;
    protected long lastFadeCheck = 0L;

    public Source(boolean bl, boolean bl2, boolean bl3, String string, FilenameURL filenameURL, SoundBuffer soundBuffer, float f, float g, float h, int i, float j, boolean bl4) {
        this.logger = SoundSystemConfig.getLogger();
        this.priority = bl;
        this.toStream = bl2;
        this.toLoop = bl3;
        this.sourcename = string;
        this.filenameURL = filenameURL;
        this.soundBuffer = soundBuffer;
        this.position = new Vector3D(f, g, h);
        this.attModel = i;
        this.distOrRoll = j;
        this.temporary = bl4;
        if (bl2 && filenameURL != null) {
            this.codec = SoundSystemConfig.getCodec(filenameURL.getFilename());
        }
    }

    public Source(Source source, SoundBuffer soundBuffer) {
        this.logger = SoundSystemConfig.getLogger();
        this.priority = source.priority;
        this.toStream = source.toStream;
        this.toLoop = source.toLoop;
        this.sourcename = source.sourcename;
        this.filenameURL = source.filenameURL;
        this.position = source.position.clone();
        this.attModel = source.attModel;
        this.distOrRoll = source.distOrRoll;
        this.temporary = source.temporary;
        this.sourceVolume = source.sourceVolume;
        this.rawDataStream = source.rawDataStream;
        this.rawDataFormat = source.rawDataFormat;
        this.soundBuffer = soundBuffer;
        if (this.toStream && this.filenameURL != null) {
            this.codec = SoundSystemConfig.getCodec(this.filenameURL.getFilename());
        }
    }

    public Source(AudioFormat audioFormat, boolean bl, String string, float f, float g, float h, int i, float j) {
        this.logger = SoundSystemConfig.getLogger();
        this.priority = bl;
        this.toStream = true;
        this.toLoop = false;
        this.sourcename = string;
        this.filenameURL = null;
        this.soundBuffer = null;
        this.position = new Vector3D(f, g, h);
        this.attModel = i;
        this.distOrRoll = j;
        this.temporary = false;
        this.rawDataStream = true;
        this.rawDataFormat = audioFormat;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void cleanup() {
        if (this.codec != null) {
            this.codec.cleanup();
        }
        Object object = this.soundSequenceLock;
        synchronized (object) {
            if (this.soundSequenceQueue != null) {
                this.soundSequenceQueue.clear();
            }
            this.soundSequenceQueue = null;
        }
        this.sourcename = null;
        this.filenameURL = null;
        this.position = null;
        this.soundBuffer = null;
        this.codec = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void queueSound(FilenameURL filenameURL) {
        if (!this.toStream) {
            this.errorMessage("Method 'queueSound' may only be used for streaming and MIDI sources.");
            return;
        }
        if (filenameURL == null) {
            this.errorMessage("File not specified in method 'queueSound'");
            return;
        }
        Object object = this.soundSequenceLock;
        synchronized (object) {
            if (this.soundSequenceQueue == null) {
                this.soundSequenceQueue = new LinkedList();
            }
            this.soundSequenceQueue.add(filenameURL);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public void dequeueSound(String string) {
        if (!this.toStream) {
            this.errorMessage("Method 'dequeueSound' may only be used for streaming and MIDI sources.");
            return;
        }
        if (string == null || string.equals("")) {
            this.errorMessage("Filename not specified in method 'dequeueSound'");
            return;
        }
        Object object = this.soundSequenceLock;
        synchronized (object) {
            if (this.soundSequenceQueue != null) {
                this.soundSequenceQueue.remove(string);
            }
        }
        object = this.soundSequenceLock;
        synchronized (object) {
            try {
                if (this.soundSequenceQueue != null) {
                    ListIterator listIterator = this.soundSequenceQueue.listIterator();
                    while (listIterator.hasNext()) {
                        if (!((FilenameURL)listIterator.next()).getFilename().equals(string)) continue;
                        listIterator.remove();
                        break;
                    }
                }
            }
            catch (Throwable throwable) {
                void throwable2;
                throw throwable2;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public void fadeOut(FilenameURL filenameURL, long l) {
        if (!this.toStream) {
            this.errorMessage("Method 'fadeOut' may only be used for streaming and MIDI sources.");
            return;
        }
        if (l < 0L) {
            this.errorMessage("Miliseconds may not be negative in method 'fadeOut'.");
            return;
        }
        this.fadeOutMilis = l;
        this.fadeInMilis = 0L;
        this.fadeOutGain = 1.0f;
        this.lastFadeCheck = System.currentTimeMillis();
        Object object = this.soundSequenceLock;
        synchronized (object) {
            try {
                if (this.soundSequenceQueue != null) {
                    this.soundSequenceQueue.clear();
                }
                if (filenameURL != null) {
                    if (this.soundSequenceQueue == null) {
                        this.soundSequenceQueue = new LinkedList();
                    }
                    this.soundSequenceQueue.add(filenameURL);
                }
                // ** MonitorExit[object] (shouldn't be in output)
            }
            catch (Throwable throwable) {
                void throwable2;
                // ** MonitorExit[object] (shouldn't be in output)
                throw throwable2;
            }
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public void fadeOutIn(FilenameURL filenameURL, long l, long m) {
        if (!this.toStream) {
            this.errorMessage("Method 'fadeOutIn' may only be used for streaming and MIDI sources.");
            return;
        }
        if (filenameURL == null) {
            this.errorMessage("Filename/URL not specified in method 'fadeOutIn'.");
            return;
        }
        if (l < 0L || m < 0L) {
            this.errorMessage("Miliseconds may not be negative in method 'fadeOutIn'.");
            return;
        }
        this.fadeOutMilis = l;
        this.fadeInMilis = m;
        this.fadeOutGain = 1.0f;
        this.lastFadeCheck = System.currentTimeMillis();
        Object object = this.soundSequenceLock;
        synchronized (object) {
            try {
                if (this.soundSequenceQueue == null) {
                    this.soundSequenceQueue = new LinkedList();
                }
                this.soundSequenceQueue.clear();
                this.soundSequenceQueue.add(filenameURL);
                // ** MonitorExit[object] (shouldn't be in output)
            }
            catch (Throwable throwable) {
                void throwable2;
                // ** MonitorExit[object] (shouldn't be in output)
                throw throwable2;
            }
            return;
        }
    }

    public boolean checkFadeOut() {
        if (!this.toStream) {
            return false;
        }
        if (this.fadeOutGain == -1.0f && this.fadeInGain == 1.0f) {
            return false;
        }
        long l = System.currentTimeMillis();
        long m = l - this.lastFadeCheck;
        this.lastFadeCheck = l;
        if (this.fadeOutGain >= 0.0f) {
            if (this.fadeOutMilis == 0L) {
                this.fadeOutGain = 0.0f;
                this.fadeInGain = 0.0f;
                if (!this.incrementSoundSequence()) {
                    this.stop();
                }
                this.positionChanged();
                this.preLoad = true;
                return false;
            }
            float f = (float)m / (float)this.fadeOutMilis;
            this.fadeOutGain -= f;
            if (this.fadeOutGain <= 0.0f) {
                this.fadeOutGain = -1.0f;
                this.fadeInGain = 0.0f;
                if (!this.incrementSoundSequence()) {
                    this.stop();
                }
                this.positionChanged();
                this.preLoad = true;
                return false;
            }
            this.positionChanged();
            return true;
        }
        if (this.fadeInGain < 1.0f) {
            this.fadeOutGain = -1.0f;
            if (this.fadeInMilis == 0L) {
                this.fadeOutGain = -1.0f;
                this.fadeInGain = 1.0f;
            } else {
                float g = (float)m / (float)this.fadeInMilis;
                this.fadeInGain += g;
                if (this.fadeInGain >= 1.0f) {
                    this.fadeOutGain = -1.0f;
                    this.fadeInGain = 1.0f;
                }
            }
            this.positionChanged();
            return true;
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean incrementSoundSequence() {
        if (!this.toStream) {
            this.errorMessage("Method 'incrementSoundSequence' may only be used for streaming and MIDI sources.");
            return false;
        }
        Object object = this.soundSequenceLock;
        synchronized (object) {
            if (this.soundSequenceQueue != null && this.soundSequenceQueue.size() > 0) {
                this.filenameURL = (FilenameURL)this.soundSequenceQueue.remove(0);
                if (this.codec != null) {
                    this.codec.cleanup();
                }
                this.codec = SoundSystemConfig.getCodec(this.filenameURL.getFilename());
                return true;
            }
        }
        return false;
    }

    public void setTemporary(boolean bl) {
        this.temporary = bl;
    }

    public void listenerMoved() {
    }

    public void setPosition(float f, float g, float h) {
        this.position.x = f;
        this.position.y = g;
        this.position.z = h;
    }

    public void positionChanged() {
    }

    public void setPriority(boolean bl) {
        this.priority = bl;
    }

    public void setLooping(boolean bl) {
        this.toLoop = bl;
    }

    public void setAttenuation(int i) {
        this.attModel = i;
    }

    public void setDistOrRoll(float f) {
        this.distOrRoll = f;
    }

    public float getDistanceFromListener() {
        return this.distanceFromListener;
    }

    public void setPitch(float f) {
        float g = f;
        if (g < 0.5f) {
            g = 0.5f;
        } else if (g > 2.0f) {
            g = 2.0f;
        }
        this.pitch = g;
    }

    public float getPitch() {
        return this.pitch;
    }

    public boolean reverseByteOrderRequired() {
        return this.reverseByteOrder;
    }

    public void changeSource(boolean bl, boolean bl2, boolean bl3, String string, FilenameURL filenameURL, SoundBuffer soundBuffer, float f, float g, float h, int i, float j, boolean bl4) {
        this.priority = bl;
        this.toStream = bl2;
        this.toLoop = bl3;
        this.sourcename = string;
        this.filenameURL = filenameURL;
        this.soundBuffer = soundBuffer;
        this.position.x = f;
        this.position.y = g;
        this.position.z = h;
        this.attModel = i;
        this.distOrRoll = j;
        this.temporary = bl4;
    }

    public int feedRawAudioData(Channel channel, byte[] bs) {
        if (!this.active(false, false)) {
            this.toPlay = true;
            return -1;
        }
        if (this.channel != channel) {
            this.channel = channel;
            this.channel.close();
            this.channel.setAudioFormat(this.rawDataFormat);
            this.positionChanged();
        }
        this.stopped(true, false);
        this.paused(true, false);
        return this.channel.feedRawAudioData(bs);
    }

    public void play(Channel channel) {
        if (!this.active(false, false)) {
            if (this.toLoop) {
                this.toPlay = true;
            }
            return;
        }
        if (this.channel != channel) {
            this.channel = channel;
            this.channel.close();
        }
        this.stopped(true, false);
        this.paused(true, false);
    }

    public boolean stream() {
        if (this.channel == null) {
            return false;
        }
        if (this.preLoad) {
            if (this.rawDataStream) {
                this.preLoad = false;
            } else {
                return this.preLoad();
            }
        }
        if (this.rawDataStream) {
            if (this.stopped() || this.paused()) {
                return true;
            }
            if (this.channel.buffersProcessed() > 0) {
                this.channel.processBuffer();
            }
        } else {
            if (this.codec == null) {
                return false;
            }
            if (this.stopped()) {
                return false;
            }
            if (this.paused()) {
                return true;
            }
            int i = this.channel.buffersProcessed();
            Object object = null;
            for (int j = 0; j < i; ++j) {
                SoundBuffer soundBuffer = this.codec.read();
                if (soundBuffer != null) {
                    if (soundBuffer.audioData != null) {
                        this.channel.queueBuffer(soundBuffer.audioData);
                    }
                    soundBuffer.cleanup();
                    soundBuffer = null;
                }
                if (!this.codec.endOfStream()) continue;
                return false;
            }
        }
        return true;
    }

    public boolean preLoad() {
        if (this.channel == null) {
            return false;
        }
        if (this.codec == null) {
            return false;
        }
        URL uRL = this.filenameURL.getURL();
        this.codec.initialize(uRL);
        Object object = null;
        for (int i = 0; i < SoundSystemConfig.getNumberStreamingBuffers(); ++i) {
            SoundBuffer soundBuffer = this.codec.read();
            if (soundBuffer == null) continue;
            if (this.soundBuffer.audioData != null) {
                this.channel.queueBuffer(this.soundBuffer.audioData);
            }
            soundBuffer.cleanup();
            soundBuffer = null;
        }
        return true;
    }

    public void pause() {
        this.toPlay = false;
        this.paused(true, true);
        if (this.channel != null) {
            this.channel.pause();
        } else {
            this.errorMessage("Channel null in method 'pause'");
        }
    }

    public void stop() {
        this.toPlay = false;
        this.stopped(true, true);
        this.paused(true, false);
        if (this.channel != null) {
            this.channel.stop();
        } else {
            this.errorMessage("Channel null in method 'stop'");
        }
    }

    public void rewind() {
        if (this.paused(false, false)) {
            this.stop();
        }
        if (this.channel != null) {
            boolean i = this.playing();
            this.channel.rewind();
            if (this.toStream && i) {
                this.stop();
                this.play(this.channel);
            }
        } else {
            this.errorMessage("Channel null in method 'rewind'");
        }
    }

    public void flush() {
        if (this.channel != null) {
            this.channel.flush();
        } else {
            this.errorMessage("Channel null in method 'flush'");
        }
    }

    public void cull() {
        if (!this.active(false, false)) {
            return;
        }
        if (this.playing() && this.toLoop) {
            this.toPlay = true;
        }
        if (this.rawDataStream) {
            this.toPlay = true;
        }
        this.active(true, false);
        if (this.channel != null) {
            this.channel.close();
        }
        this.channel = null;
    }

    public void activate() {
        this.active(true, true);
    }

    public boolean active() {
        return this.active(false, false);
    }

    public boolean playing() {
        if (this.channel == null || this.channel.attachedSource != this) {
            return false;
        }
        if (this.paused() || this.stopped()) {
            return false;
        }
        return this.channel.playing();
    }

    public boolean stopped() {
        return this.stopped(false, false);
    }

    public boolean paused() {
        return this.paused(false, false);
    }

    private synchronized boolean active(boolean bl, boolean bl2) {
        if (bl) {
            this.active = bl2;
        }
        return this.active;
    }

    private synchronized boolean stopped(boolean bl, boolean bl2) {
        if (bl) {
            this.stopped = bl2;
        }
        return this.stopped;
    }

    private synchronized boolean paused(boolean bl, boolean bl2) {
        if (bl) {
            this.paused = bl2;
        }
        return this.paused;
    }

    public String getClassName() {
        String string = SoundSystemConfig.getLibraryTitle(this.libraryType);
        if (string.equals("No Sound")) {
            return "Source";
        }
        return "Source" + string;
    }

    protected void message(String string) {
        this.logger.message(string, 0);
    }

    protected void importantMessage(String string) {
        this.logger.importantMessage(string, 0);
    }

    protected boolean errorCheck(boolean bl, String string) {
        return this.logger.errorCheck(bl, this.getClassName(), string, 0);
    }

    protected void errorMessage(String string) {
        this.logger.errorMessage(this.getClassName(), string, 0);
    }

    protected void printStackTrace(Exception exception) {
        this.logger.printStackTrace(exception, 1);
    }
}

