/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.openal.AL10
 */
package paulscode.sound.libraries;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import paulscode.sound.Channel;
import paulscode.sound.FilenameURL;
import paulscode.sound.SoundBuffer;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.Source;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

@Environment(value=EnvType.CLIENT)
public class SourceLWJGLOpenAL
extends Source {
    private ChannelLWJGLOpenAL channelOpenAL;
    private IntBuffer myBuffer;
    private FloatBuffer listenerPosition;
    private FloatBuffer sourcePosition;
    private FloatBuffer sourceVelocity;

    public SourceLWJGLOpenAL(FloatBuffer floatBuffer, IntBuffer intBuffer, boolean bl, boolean bl2, boolean bl3, String string, FilenameURL filenameURL, SoundBuffer soundBuffer, float f, float g, float h, int i, float j, boolean bl4) {
        super(bl, bl2, bl3, string, filenameURL, soundBuffer, f, g, h, i, j, bl4);
        this.channelOpenAL = (ChannelLWJGLOpenAL)this.channel;
        this.reverseByteOrder = true;
        if (this.codec != null) {
            this.codec.reverseByteOrder(true);
        }
        this.listenerPosition = floatBuffer;
        this.myBuffer = intBuffer;
        this.libraryType = LibraryLWJGLOpenAL.class;
        this.pitch = 1.0f;
        this.resetALInformation();
    }

    public SourceLWJGLOpenAL(FloatBuffer floatBuffer, IntBuffer intBuffer, Source source, SoundBuffer soundBuffer) {
        super(source, soundBuffer);
        this.channelOpenAL = (ChannelLWJGLOpenAL)this.channel;
        this.reverseByteOrder = true;
        if (this.codec != null) {
            this.codec.reverseByteOrder(true);
        }
        this.listenerPosition = floatBuffer;
        this.myBuffer = intBuffer;
        this.libraryType = LibraryLWJGLOpenAL.class;
        this.pitch = 1.0f;
        this.resetALInformation();
    }

    public SourceLWJGLOpenAL(FloatBuffer floatBuffer, AudioFormat audioFormat, boolean bl, String string, float f, float g, float h, int i, float j) {
        super(audioFormat, bl, string, f, g, h, i, j);
        this.channelOpenAL = (ChannelLWJGLOpenAL)this.channel;
        this.reverseByteOrder = true;
        this.listenerPosition = floatBuffer;
        this.libraryType = LibraryLWJGLOpenAL.class;
        this.pitch = 1.0f;
        this.resetALInformation();
    }

    public void cleanup() {
        super.cleanup();
    }

    public void changeSource(FloatBuffer floatBuffer, IntBuffer intBuffer, boolean bl, boolean bl2, boolean bl3, String string, FilenameURL filenameURL, SoundBuffer soundBuffer, float f, float g, float h, int i, float j, boolean bl4) {
        super.changeSource(bl, bl2, bl3, string, filenameURL, soundBuffer, f, g, h, i, j, bl4);
        this.reverseByteOrder = true;
        this.listenerPosition = floatBuffer;
        this.myBuffer = intBuffer;
        this.pitch = 1.0f;
        this.resetALInformation();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean incrementSoundSequence() {
        if (!this.toStream) {
            this.errorMessage("Method 'incrementSoundSequence' may only be used for streaming sources.");
            return false;
        }
        Object object = this.soundSequenceLock;
        synchronized (object) {
            AudioFormat audioFormat;
            if (this.soundSequenceQueue == null) return false;
            if (this.soundSequenceQueue.size() <= 0) return false;
            this.filenameURL = (FilenameURL)this.soundSequenceQueue.remove(0);
            if (this.codec != null) {
                this.codec.cleanup();
            }
            this.codec = SoundSystemConfig.getCodec(this.filenameURL.getFilename());
            if (this.codec == null) return true;
            this.codec.reverseByteOrder(true);
            if (this.codec.getAudioFormat() == null) {
                this.codec.initialize(this.filenameURL.getURL());
            }
            if ((audioFormat = this.codec.getAudioFormat()) == null) {
                this.errorMessage("Audio Format null in method 'incrementSoundSequence'");
                return false;
            }
            int i = 0;
            if (audioFormat.getChannels() == 1) {
                if (audioFormat.getSampleSizeInBits() == 8) {
                    i = 4352;
                } else {
                    if (audioFormat.getSampleSizeInBits() != 16) {
                        this.errorMessage("Illegal sample size in method 'incrementSoundSequence'");
                        return false;
                    }
                    i = 4353;
                }
            } else {
                if (audioFormat.getChannels() != 2) {
                    this.errorMessage("Audio data neither mono nor stereo in method 'incrementSoundSequence'");
                    return false;
                }
                if (audioFormat.getSampleSizeInBits() == 8) {
                    i = 4354;
                } else {
                    if (audioFormat.getSampleSizeInBits() != 16) {
                        this.errorMessage("Illegal sample size in method 'incrementSoundSequence'");
                        return false;
                    }
                    i = 4355;
                }
            }
            this.channelOpenAL.setFormat(i, (int)audioFormat.getSampleRate());
            this.preLoad = true;
            return true;
        }
    }

    public void listenerMoved() {
        this.positionChanged();
    }

    public void setPosition(float f, float g, float h) {
        super.setPosition(f, g, h);
        if (this.sourcePosition == null) {
            this.resetALInformation();
        } else {
            this.positionChanged();
        }
        this.sourcePosition.put(0, f);
        this.sourcePosition.put(1, g);
        this.sourcePosition.put(2, h);
        if (this.channel != null && this.channel.attachedSource == this && this.channelOpenAL != null && this.channelOpenAL.ALSource != null) {
            AL10.alSource((int)this.channelOpenAL.ALSource.get(0), (int)4100, (FloatBuffer)this.sourcePosition);
            this.checkALError();
        }
    }

    public void positionChanged() {
        this.calculateDistance();
        this.calculateGain();
        if (this.channel != null && this.channel.attachedSource == this && this.channelOpenAL != null && this.channelOpenAL.ALSource != null) {
            AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4106, (float)(this.gain * this.sourceVolume * Math.abs(this.fadeOutGain) * this.fadeInGain));
            this.checkALError();
        }
        this.checkPitch();
    }

    private void checkPitch() {
        if (this.channel != null && this.channel.attachedSource == this && LibraryLWJGLOpenAL.alPitchSupported() && this.channelOpenAL != null && this.channelOpenAL.ALSource != null) {
            AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4099, (float)this.pitch);
            this.checkALError();
        }
    }

    public void setLooping(boolean bl) {
        super.setLooping(bl);
        if (this.channel != null && this.channel.attachedSource == this && this.channelOpenAL != null && this.channelOpenAL.ALSource != null) {
            if (bl) {
                AL10.alSourcei((int)this.channelOpenAL.ALSource.get(0), (int)4103, (int)1);
            } else {
                AL10.alSourcei((int)this.channelOpenAL.ALSource.get(0), (int)4103, (int)0);
            }
            this.checkALError();
        }
    }

    public void setAttenuation(int i) {
        super.setAttenuation(i);
        if (this.channel != null && this.channel.attachedSource == this && this.channelOpenAL != null && this.channelOpenAL.ALSource != null) {
            if (i == 1) {
                AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4129, (float)this.distOrRoll);
            } else {
                AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4129, (float)0.0f);
            }
            this.checkALError();
        }
    }

    public void setDistOrRoll(float f) {
        super.setDistOrRoll(f);
        if (this.channel != null && this.channel.attachedSource == this && this.channelOpenAL != null && this.channelOpenAL.ALSource != null) {
            if (this.attModel == 1) {
                AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4129, (float)f);
            } else {
                AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4129, (float)0.0f);
            }
            this.checkALError();
        }
    }

    public void setPitch(float f) {
        super.setPitch(f);
        this.checkPitch();
    }

    /*
     * Enabled aggressive block sorting
     */
    public void play(Channel channel) {
        boolean i;
        if (!this.active()) {
            if (this.toLoop) {
                this.toPlay = true;
            }
            return;
        }
        if (channel == null) {
            this.errorMessage("Unable to play source, because channel was null");
            return;
        }
        boolean bl = i = this.channel != channel;
        if (this.channel != null && this.channel.attachedSource != this) {
            i = true;
        }
        boolean j = this.paused();
        super.play(channel);
        this.channelOpenAL = (ChannelLWJGLOpenAL)this.channel;
        if (i) {
            this.setPosition(this.position.x, this.position.y, this.position.z);
            this.checkPitch();
            if (this.channelOpenAL != null && this.channelOpenAL.ALSource != null) {
                if (LibraryLWJGLOpenAL.alPitchSupported()) {
                    AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4099, (float)this.pitch);
                    this.checkALError();
                }
                AL10.alSource((int)this.channelOpenAL.ALSource.get(0), (int)4100, (FloatBuffer)this.sourcePosition);
                this.checkALError();
                AL10.alSource((int)this.channelOpenAL.ALSource.get(0), (int)4102, (FloatBuffer)this.sourceVelocity);
                this.checkALError();
                if (this.attModel == 1) {
                    AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4129, (float)this.distOrRoll);
                } else {
                    AL10.alSourcef((int)this.channelOpenAL.ALSource.get(0), (int)4129, (float)0.0f);
                }
                this.checkALError();
                if (this.toLoop && !this.toStream) {
                    AL10.alSourcei((int)this.channelOpenAL.ALSource.get(0), (int)4103, (int)1);
                } else {
                    AL10.alSourcei((int)this.channelOpenAL.ALSource.get(0), (int)4103, (int)0);
                }
                this.checkALError();
            }
            if (!this.toStream) {
                if (this.myBuffer == null) {
                    this.errorMessage("No sound buffer to play");
                    return;
                }
                this.channelOpenAL.attachBuffer(this.myBuffer);
            }
        }
        if (!this.playing()) {
            if (this.toStream && !j) {
                AudioFormat audioFormat;
                if (this.codec == null) {
                    this.errorMessage("Decoder null in method 'play'");
                    return;
                }
                if (this.codec.getAudioFormat() == null) {
                    this.codec.initialize(this.filenameURL.getURL());
                }
                if ((audioFormat = this.codec.getAudioFormat()) == null) {
                    this.errorMessage("Audio Format null in method 'play'");
                    return;
                }
                int k = 0;
                if (audioFormat.getChannels() == 1) {
                    if (audioFormat.getSampleSizeInBits() == 8) {
                        k = 4352;
                    } else {
                        if (audioFormat.getSampleSizeInBits() != 16) {
                            this.errorMessage("Illegal sample size in method 'play'");
                            return;
                        }
                        k = 4353;
                    }
                } else {
                    if (audioFormat.getChannels() != 2) {
                        this.errorMessage("Audio data neither mono nor stereo in method 'play'");
                        return;
                    }
                    if (audioFormat.getSampleSizeInBits() == 8) {
                        k = 4354;
                    } else {
                        if (audioFormat.getSampleSizeInBits() != 16) {
                            this.errorMessage("Illegal sample size in method 'play'");
                            return;
                        }
                        k = 4355;
                    }
                }
                this.channelOpenAL.setFormat(k, (int)audioFormat.getSampleRate());
                this.preLoad = true;
            }
            this.channel.play();
            if (this.pitch != 1.0f) {
                this.checkPitch();
            }
        }
    }

    public boolean preLoad() {
        if (this.codec == null) {
            return false;
        }
        this.codec.initialize(this.filenameURL.getURL());
        LinkedList<byte[]> linkedList = new LinkedList<byte[]>();
        for (int i = 0; i < SoundSystemConfig.getNumberStreamingBuffers(); ++i) {
            this.soundBuffer = this.codec.read();
            if (this.soundBuffer == null || this.soundBuffer.audioData == null) break;
            linkedList.add(this.soundBuffer.audioData);
        }
        this.positionChanged();
        this.channel.preLoadBuffers(linkedList);
        this.preLoad = false;
        return true;
    }

    private void resetALInformation() {
        this.sourcePosition = BufferUtils.createFloatBuffer((int)3).put(new float[]{this.position.x, this.position.y, this.position.z});
        this.sourceVelocity = BufferUtils.createFloatBuffer((int)3).put(new float[]{0.0f, 0.0f, 0.0f});
        this.sourcePosition.flip();
        this.sourceVelocity.flip();
        this.positionChanged();
    }

    private void calculateDistance() {
        if (this.listenerPosition != null) {
            double d = this.position.x - this.listenerPosition.get(0);
            double e = this.position.y - this.listenerPosition.get(1);
            double f = this.position.z - this.listenerPosition.get(2);
            this.distanceFromListener = (float)Math.sqrt(d * d + e * e + f * f);
        }
    }

    private void calculateGain() {
        if (this.attModel == 2) {
            this.gain = this.distanceFromListener <= 0.0f ? 1.0f : (this.distanceFromListener >= this.distOrRoll ? 0.0f : 1.0f - this.distanceFromListener / this.distOrRoll);
            if (this.gain > 1.0f) {
                this.gain = 1.0f;
            }
            if (this.gain < 0.0f) {
                this.gain = 0.0f;
            }
        } else {
            this.gain = 1.0f;
        }
    }

    private boolean checkALError() {
        switch (AL10.alGetError()) {
            case 0: {
                return false;
            }
            case 40961: {
                this.errorMessage("Invalid name parameter.");
                return true;
            }
            case 40962: {
                this.errorMessage("Invalid parameter.");
                return true;
            }
            case 40963: {
                this.errorMessage("Invalid enumerated parameter value.");
                return true;
            }
            case 40964: {
                this.errorMessage("Illegal call.");
                return true;
            }
            case 40965: {
                this.errorMessage("Unable to allocate memory.");
                return true;
            }
        }
        this.errorMessage("An unrecognized error occurred.");
        return true;
    }
}

