/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package paulscode.sound;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import paulscode.sound.FilenameURL;
import paulscode.sound.SimpleThread;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;
import paulscode.sound.SoundSystemLogger;

@Environment(value=EnvType.CLIENT)
public class MidiChannel
implements MetaEventListener {
    private SoundSystemLogger logger;
    private FilenameURL filenameURL;
    private String sourcename;
    private static final int CHANGE_VOLUME = 7;
    private static final int END_OF_TRACK = 47;
    private static final boolean GET = false;
    private static final boolean SET = true;
    private static final boolean XXX = false;
    private Sequencer sequencer = null;
    private Synthesizer synthesizer = null;
    private MidiDevice synthDevice = null;
    private Sequence sequence = null;
    private boolean toLoop = true;
    private float gain = 1.0f;
    private boolean loading = true;
    private LinkedList sequenceQueue = null;
    private final Object sequenceQueueLock = new Object();
    protected float fadeOutGain = -1.0f;
    protected float fadeInGain = 1.0f;
    protected long fadeOutMilis = 0L;
    protected long fadeInMilis = 0L;
    protected long lastFadeCheck = 0L;
    private FadeThread fadeThread = null;

    public MidiChannel(boolean bl, String string, String string2) {
        this.loading(true, true);
        this.logger = SoundSystemConfig.getLogger();
        this.filenameURL(true, new FilenameURL(string2));
        this.sourcename(true, string);
        this.setLooping(bl);
        this.init();
        this.loading(true, false);
    }

    public MidiChannel(boolean bl, String string, URL uRL, String string2) {
        this.loading(true, true);
        this.logger = SoundSystemConfig.getLogger();
        this.filenameURL(true, new FilenameURL(uRL, string2));
        this.sourcename(true, string);
        this.setLooping(bl);
        this.init();
        this.loading(true, false);
    }

    public MidiChannel(boolean bl, String string, FilenameURL filenameURL) {
        this.loading(true, true);
        this.logger = SoundSystemConfig.getLogger();
        this.filenameURL(true, filenameURL);
        this.sourcename(true, string);
        this.setLooping(bl);
        this.init();
        this.loading(true, false);
    }

    private void init() {
        this.getSequencer();
        this.setSequence(this.filenameURL(false, null).getURL());
        this.getSynthesizer();
        this.resetGain();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void cleanup() {
        this.loading(true, true);
        this.setLooping(true);
        if (this.sequencer != null) {
            try {
                this.sequencer.stop();
                this.sequencer.close();
                this.sequencer.removeMetaEventListener(this);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.logger = null;
        this.sequencer = null;
        this.synthesizer = null;
        this.sequence = null;
        Object object = this.sequenceQueueLock;
        synchronized (object) {
            if (this.sequenceQueue != null) {
                this.sequenceQueue.clear();
            }
            this.sequenceQueue = null;
        }
        if (this.fadeThread != null) {
            boolean i = false;
            try {
                this.fadeThread.kill();
                this.fadeThread.interrupt();
            }
            catch (Exception exception) {
                i = true;
            }
            if (!i) {
                for (int j = 0; j < 50 && this.fadeThread.alive(); ++j) {
                    try {
                        Thread.sleep(100L);
                        continue;
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                }
            }
            if (i || this.fadeThread.alive()) {
                this.errorMessage("MIDI fade effects thread did not die!");
                this.message("Ignoring errors... continuing clean-up.");
            }
        }
        this.fadeThread = null;
        this.loading(true, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void queueSound(FilenameURL filenameURL) {
        if (filenameURL == null) {
            this.errorMessage("Filename/URL not specified in method 'queueSound'");
            return;
        }
        Object object = this.sequenceQueueLock;
        synchronized (object) {
            if (this.sequenceQueue == null) {
                this.sequenceQueue = new LinkedList();
            }
            this.sequenceQueue.add(filenameURL);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public void dequeueSound(String string) {
        if (string == null || string.equals("")) {
            this.errorMessage("Filename not specified in method 'dequeueSound'");
            return;
        }
        Object object = this.sequenceQueueLock;
        synchronized (object) {
            try {
                if (this.sequenceQueue != null) {
                    ListIterator listIterator = this.sequenceQueue.listIterator();
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
        if (l < 0L) {
            this.errorMessage("Miliseconds may not be negative in method 'fadeOut'.");
            return;
        }
        this.fadeOutMilis = l;
        this.fadeInMilis = 0L;
        this.fadeOutGain = 1.0f;
        this.lastFadeCheck = System.currentTimeMillis();
        Object object = this.sequenceQueueLock;
        synchronized (object) {
            try {
                if (this.sequenceQueue != null) {
                    this.sequenceQueue.clear();
                }
                if (filenameURL != null) {
                    if (this.sequenceQueue == null) {
                        this.sequenceQueue = new LinkedList();
                    }
                    this.sequenceQueue.add(filenameURL);
                }
                // ** MonitorExit[object] (shouldn't be in output)
            }
            catch (Throwable throwable) {
                void throwable2;
                // ** MonitorExit[object] (shouldn't be in output)
                throw throwable2;
            }
            if (this.fadeThread == null) {
                this.fadeThread = new FadeThread();
                this.fadeThread.start();
            }
            this.fadeThread.interrupt();
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public void fadeOutIn(FilenameURL filenameURL, long l, long m) {
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
        Object object = this.sequenceQueueLock;
        synchronized (object) {
            try {
                if (this.sequenceQueue == null) {
                    this.sequenceQueue = new LinkedList();
                }
                this.sequenceQueue.clear();
                this.sequenceQueue.add(filenameURL);
                // ** MonitorExit[object] (shouldn't be in output)
            }
            catch (Throwable throwable) {
                void throwable2;
                // ** MonitorExit[object] (shouldn't be in output)
                throw throwable2;
            }
            if (this.fadeThread == null) {
                this.fadeThread = new FadeThread();
                this.fadeThread.start();
            }
            this.fadeThread.interrupt();
            return;
        }
    }

    private synchronized boolean checkFadeOut() {
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
                if (!this.incrementSequence()) {
                    this.stop();
                }
                this.rewind();
                this.resetGain();
                return false;
            }
            float f = (float)m / (float)this.fadeOutMilis;
            this.fadeOutGain -= f;
            if (this.fadeOutGain <= 0.0f) {
                this.fadeOutGain = -1.0f;
                this.fadeInGain = 0.0f;
                if (!this.incrementSequence()) {
                    this.stop();
                }
                this.rewind();
                this.resetGain();
                return false;
            }
            this.resetGain();
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
            this.resetGain();
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean incrementSequence() {
        Object object = this.sequenceQueueLock;
        synchronized (object) {
            if (this.sequenceQueue != null && this.sequenceQueue.size() > 0) {
                this.filenameURL(true, (FilenameURL)this.sequenceQueue.remove(0));
                this.loading(true, true);
                if (this.sequencer == null) {
                    this.getSequencer();
                } else {
                    this.sequencer.stop();
                    this.sequencer.setMicrosecondPosition(0L);
                    this.sequencer.removeMetaEventListener(this);
                    try {
                        Thread.sleep(100L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                }
                if (this.sequencer == null) {
                    this.errorMessage("Unable to set the sequence in method 'incrementSequence', because there wasn't a sequencer to use.");
                    this.loading(true, false);
                    return false;
                }
                this.setSequence(this.filenameURL(false, null).getURL());
                this.sequencer.start();
                this.resetGain();
                this.sequencer.addMetaEventListener(this);
                this.loading(true, false);
                return true;
            }
        }
        return false;
    }

    public void play() {
        if (!this.loading()) {
            if (this.sequencer == null) {
                return;
            }
            try {
                this.sequencer.start();
                this.sequencer.addMetaEventListener(this);
            }
            catch (Exception exception) {
                this.errorMessage("Exception in method 'play'");
                this.printStackTrace(exception);
                SoundSystemException soundSystemException = new SoundSystemException(exception.getMessage());
                SoundSystem.setException(soundSystemException);
            }
        }
    }

    public void stop() {
        if (!this.loading()) {
            if (this.sequencer == null) {
                return;
            }
            try {
                this.sequencer.stop();
                this.sequencer.setMicrosecondPosition(0L);
                this.sequencer.removeMetaEventListener(this);
            }
            catch (Exception exception) {
                this.errorMessage("Exception in method 'stop'");
                this.printStackTrace(exception);
                SoundSystemException soundSystemException = new SoundSystemException(exception.getMessage());
                SoundSystem.setException(soundSystemException);
            }
        }
    }

    public void pause() {
        if (!this.loading()) {
            if (this.sequencer == null) {
                return;
            }
            try {
                this.sequencer.stop();
            }
            catch (Exception exception) {
                this.errorMessage("Exception in method 'pause'");
                this.printStackTrace(exception);
                SoundSystemException soundSystemException = new SoundSystemException(exception.getMessage());
                SoundSystem.setException(soundSystemException);
            }
        }
    }

    public void rewind() {
        if (!this.loading()) {
            if (this.sequencer == null) {
                return;
            }
            try {
                this.sequencer.setMicrosecondPosition(0L);
            }
            catch (Exception exception) {
                this.errorMessage("Exception in method 'rewind'");
                this.printStackTrace(exception);
                SoundSystemException soundSystemException = new SoundSystemException(exception.getMessage());
                SoundSystem.setException(soundSystemException);
            }
        }
    }

    public void setVolume(float f) {
        this.gain = f;
        this.resetGain();
    }

    public float getVolume() {
        return this.gain;
    }

    public void switchSource(boolean bl, String string, String string2) {
        this.loading(true, true);
        this.filenameURL(true, new FilenameURL(string2));
        this.sourcename(true, string);
        this.setLooping(bl);
        this.reset();
        this.loading(true, false);
    }

    public void switchSource(boolean bl, String string, URL uRL, String string2) {
        this.loading(true, true);
        this.filenameURL(true, new FilenameURL(uRL, string2));
        this.sourcename(true, string);
        this.setLooping(bl);
        this.reset();
        this.loading(true, false);
    }

    public void switchSource(boolean bl, String string, FilenameURL filenameURL) {
        this.loading(true, true);
        this.filenameURL(true, filenameURL);
        this.sourcename(true, string);
        this.setLooping(bl);
        this.reset();
        this.loading(true, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void reset() {
        Object object2 = this.sequenceQueueLock;
        synchronized (object2) {
            if (this.sequenceQueue != null) {
                this.sequenceQueue.clear();
            }
        }
        if (this.sequencer == null) {
            this.getSequencer();
        } else {
            this.sequencer.stop();
            this.sequencer.setMicrosecondPosition(0L);
            this.sequencer.removeMetaEventListener(this);
            try {
                Thread.sleep(100L);
            }
            catch (InterruptedException object2) {
                // empty catch block
            }
        }
        if (this.sequencer == null) {
            this.errorMessage("Unable to set the sequence in method 'reset', because there wasn't a sequencer to use.");
            return;
        }
        this.setSequence(this.filenameURL(false, null).getURL());
        this.sequencer.start();
        this.resetGain();
        this.sequencer.addMetaEventListener(this);
    }

    public void setLooping(boolean bl) {
        this.toLoop(true, bl);
    }

    public boolean getLooping() {
        return this.toLoop(false, false);
    }

    private synchronized boolean toLoop(boolean bl, boolean bl2) {
        if (bl) {
            this.toLoop = bl2;
        }
        return this.toLoop;
    }

    public boolean loading() {
        return this.loading(false, false);
    }

    private synchronized boolean loading(boolean bl, boolean bl2) {
        if (bl) {
            this.loading = bl2;
        }
        return this.loading;
    }

    public void setSourcename(String string) {
        this.sourcename(true, string);
    }

    public String getSourcename() {
        return this.sourcename(false, null);
    }

    private synchronized String sourcename(boolean bl, String string) {
        if (bl) {
            this.sourcename = string;
        }
        return this.sourcename;
    }

    public void setFilenameURL(FilenameURL filenameURL) {
        this.filenameURL(true, filenameURL);
    }

    public String getFilename() {
        return this.filenameURL(false, null).getFilename();
    }

    public FilenameURL getFilenameURL() {
        return this.filenameURL(false, null);
    }

    private synchronized FilenameURL filenameURL(boolean bl, FilenameURL filenameURL) {
        if (bl) {
            this.filenameURL = filenameURL;
        }
        return this.filenameURL;
    }

    public void meta(MetaMessage metaMessage) {
        if (metaMessage.getType() == 47) {
            if (this.toLoop) {
                if (!this.checkFadeOut()) {
                    if (!this.incrementSequence()) {
                        try {
                            this.sequencer.setMicrosecondPosition(0L);
                            this.sequencer.start();
                            this.resetGain();
                        }
                        catch (Exception exception) {}
                    }
                } else if (this.sequencer != null) {
                    try {
                        this.sequencer.setMicrosecondPosition(0L);
                        this.sequencer.start();
                        this.resetGain();
                    }
                    catch (Exception exception) {}
                }
            } else if (!this.checkFadeOut()) {
                if (!this.incrementSequence()) {
                    try {
                        this.sequencer.stop();
                        this.sequencer.setMicrosecondPosition(0L);
                        this.sequencer.removeMetaEventListener(this);
                    }
                    catch (Exception exception2) {}
                }
            } else {
                try {
                    this.sequencer.stop();
                    this.sequencer.setMicrosecondPosition(0L);
                    this.sequencer.removeMetaEventListener(this);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
    }

    public void resetGain() {
        if (this.gain < 0.0f) {
            this.gain = 0.0f;
        }
        if (this.gain > 1.0f) {
            this.gain = 1.0f;
        }
        int i = (int)(this.gain * SoundSystemConfig.getMasterGain() * Math.abs(this.fadeOutGain) * this.fadeInGain * 127.0f);
        if (this.synthesizer != null) {
            javax.sound.midi.MidiChannel[] midiChannels = this.synthesizer.getChannels();
            for (int j = 0; midiChannels != null && j < midiChannels.length; ++j) {
                midiChannels[j].controlChange(7, i);
            }
        } else if (this.synthDevice != null) {
            try {
                ShortMessage shortMessage = new ShortMessage();
                for (int k = 0; k < 16; ++k) {
                    shortMessage.setMessage(176, k, 7, i);
                    this.synthDevice.getReceiver().send(shortMessage, -1L);
                }
            }
            catch (Exception exception) {
                this.errorMessage("Error resetting gain on MIDI device");
                this.printStackTrace(exception);
            }
        } else if (this.sequencer != null && this.sequencer instanceof Synthesizer) {
            this.synthesizer = (Synthesizer)((Object)this.sequencer);
            javax.sound.midi.MidiChannel[] midiChannels2 = this.synthesizer.getChannels();
            for (int l = 0; midiChannels2 != null && l < midiChannels2.length; ++l) {
                midiChannels2[l].controlChange(7, i);
            }
        } else {
            try {
                Receiver receiver = MidiSystem.getReceiver();
                ShortMessage shortMessage2 = new ShortMessage();
                for (int m = 0; m < 16; ++m) {
                    shortMessage2.setMessage(176, m, 7, i);
                    receiver.send(shortMessage2, -1L);
                }
            }
            catch (Exception exception2) {
                this.errorMessage("Error resetting gain on default receiver");
                this.printStackTrace(exception2);
            }
        }
    }

    private void getSequencer() {
        block7: {
            try {
                this.sequencer = MidiSystem.getSequencer();
                if (this.sequencer == null) break block7;
                try {
                    this.sequencer.getTransmitter();
                }
                catch (MidiUnavailableException midiUnavailableException) {
                    this.message("Unable to get a transmitter from the default MIDI sequencer");
                }
                this.sequencer.open();
            }
            catch (MidiUnavailableException midiUnavailableException2) {
                this.message("Unable to open the default MIDI sequencer");
                this.sequencer = null;
            }
        }
        if (this.sequencer == null) {
            this.sequencer = this.openSequencer("Real Time Sequencer");
        }
        if (this.sequencer == null) {
            this.sequencer = this.openSequencer("Java Sound Sequencer");
        }
        if (this.sequencer == null) {
            this.errorMessage("Failed to find an available MIDI sequencer");
            return;
        }
    }

    private void setSequence(URL uRL) {
        if (this.sequencer == null) {
            this.errorMessage("Unable to update the sequence in method 'setSequence', because variable 'sequencer' is null");
            return;
        }
        if (uRL == null) {
            this.errorMessage("Unable to load Midi file in method 'setSequence'.");
            return;
        }
        try {
            this.sequence = MidiSystem.getSequence(uRL);
        }
        catch (IOException iOException) {
            this.errorMessage("Input failed while reading from MIDI file in method 'setSequence'.");
            this.printStackTrace(iOException);
            return;
        }
        catch (InvalidMidiDataException invalidMidiDataException) {
            this.errorMessage("Invalid MIDI data encountered, or not a MIDI file in method 'setSequence' (1).");
            this.printStackTrace(invalidMidiDataException);
            return;
        }
        if (this.sequence == null) {
            this.errorMessage("MidiSystem 'getSequence' method returned null in method 'setSequence'.");
        } else {
            try {
                this.sequencer.setSequence(this.sequence);
            }
            catch (InvalidMidiDataException invalidMidiDataException2) {
                this.errorMessage("Invalid MIDI data encountered, or not a MIDI file in method 'setSequence' (2).");
                this.printStackTrace(invalidMidiDataException2);
                return;
            }
            catch (Exception exception) {
                this.errorMessage("Problem setting sequence from MIDI file in method 'setSequence'.");
                this.printStackTrace(exception);
                return;
            }
        }
    }

    private void getSynthesizer() {
        if (this.sequencer == null) {
            this.errorMessage("Unable to load a Synthesizer in method 'getSynthesizer', because variable 'sequencer' is null");
            return;
        }
        if (this.sequencer instanceof Synthesizer) {
            this.synthesizer = (Synthesizer)((Object)this.sequencer);
        } else {
            try {
                this.synthesizer = MidiSystem.getSynthesizer();
                this.synthesizer.open();
            }
            catch (MidiUnavailableException midiUnavailableException) {
                this.message("Unable to open the default synthesizer");
                this.synthesizer = null;
            }
            if (this.synthesizer == null) {
                this.synthDevice = this.openMidiDevice("Java Sound Synthesizer");
                if (this.synthDevice == null) {
                    this.synthDevice = this.openMidiDevice("Microsoft GS Wavetable");
                }
                if (this.synthDevice == null) {
                    this.synthDevice = this.openMidiDevice("Gervill");
                }
                if (this.synthDevice == null) {
                    this.errorMessage("Failed to find an available MIDI synthesizer");
                    return;
                }
            }
            if (this.synthesizer == null) {
                try {
                    this.sequencer.getTransmitter().setReceiver(this.synthDevice.getReceiver());
                }
                catch (MidiUnavailableException midiUnavailableException2) {
                    this.errorMessage("Unable to link sequencer transmitter with MIDI device receiver");
                }
            } else if (this.synthesizer.getDefaultSoundbank() == null) {
                try {
                    this.sequencer.getTransmitter().setReceiver(MidiSystem.getReceiver());
                }
                catch (MidiUnavailableException midiUnavailableException3) {
                    this.errorMessage("Unable to link sequencer transmitter with default receiver");
                }
            } else {
                try {
                    this.sequencer.getTransmitter().setReceiver(this.synthesizer.getReceiver());
                }
                catch (MidiUnavailableException midiUnavailableException4) {
                    this.errorMessage("Unable to link sequencer transmitter with synthesizer receiver");
                }
            }
        }
    }

    /*
     * WARNING - void declaration
     */
    private Sequencer openSequencer(String string) {
        void sequencer2;
        Object object = null;
        Sequencer sequencer = (Sequencer)this.openMidiDevice(string);
        if (sequencer == null) {
            return null;
        }
        try {
            sequencer.getTransmitter();
        }
        catch (MidiUnavailableException midiUnavailableException) {
            this.message("    Unable to get a transmitter from this sequencer");
            Object object2 = null;
            return null;
        }
        return sequencer2;
    }

    private MidiDevice openMidiDevice(String string) {
        this.message("Searching for MIDI device with name containing '" + string + "'");
        Object object = null;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < infos.length; ++i) {
            MidiDevice midiDevice2;
            Object object2 = null;
            try {
                midiDevice2 = MidiSystem.getMidiDevice(infos[i]);
            }
            catch (MidiUnavailableException midiUnavailableException) {
                this.message("    Problem in method 'getMidiDevice':  MIDIUnavailableException was thrown");
                midiDevice2 = null;
            }
            if (midiDevice2 == null || !infos[i].getName().contains(string)) continue;
            this.message("    Found MIDI device named '" + infos[i].getName() + "'");
            if (midiDevice2 instanceof Synthesizer) {
                this.message("        *this is a Synthesizer instance");
            }
            if (midiDevice2 instanceof Sequencer) {
                this.message("        *this is a Sequencer instance");
            }
            try {
                midiDevice2.open();
            }
            catch (MidiUnavailableException midiUnavailableException2) {
                this.message("    Unable to open this MIDI device");
                midiDevice2 = null;
            }
            return midiDevice2;
        }
        this.message("    MIDI device not found");
        return null;
    }

    protected void message(String string) {
        this.logger.message(string, 0);
    }

    protected void importantMessage(String string) {
        this.logger.importantMessage(string, 0);
    }

    protected boolean errorCheck(boolean bl, String string) {
        return this.logger.errorCheck(bl, "MidiChannel", string, 0);
    }

    protected void errorMessage(String string) {
        this.logger.errorMessage("MidiChannel", string, 0);
    }

    protected void printStackTrace(Exception exception) {
        this.logger.printStackTrace(exception, 1);
    }

    @Environment(value=EnvType.CLIENT)
    class FadeThread
    extends SimpleThread {
        private FadeThread() {
        }

        public void run() {
            while (!this.dying()) {
                if (MidiChannel.this.fadeOutGain == -1.0f && MidiChannel.this.fadeInGain == 1.0f) {
                    this.snooze(3600000L);
                }
                MidiChannel.this.checkFadeOut();
                this.snooze(50L);
            }
            this.cleanup();
        }
    }
}

