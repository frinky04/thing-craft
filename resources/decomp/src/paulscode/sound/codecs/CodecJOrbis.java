/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package paulscode.sound.codecs;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import paulscode.sound.ICodec;
import paulscode.sound.SoundBuffer;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemLogger;

@Environment(value=EnvType.CLIENT)
public class CodecJOrbis
implements ICodec {
    private static final boolean GET = false;
    private static final boolean SET = true;
    private static final boolean XXX = false;
    protected URL url;
    protected URLConnection urlConnection = null;
    private InputStream inputStream;
    private AudioFormat audioFormat;
    private boolean endOfStream = false;
    private boolean initialized = false;
    private byte[] buffer = null;
    private int bufferSize;
    private int count = 0;
    private int index = 0;
    private int convertedBufferSize;
    private float[][][] pcmInfo;
    private int[] pcmIndex;
    private Packet joggPacket = new Packet();
    private Page joggPage = new Page();
    private StreamState joggStreamState = new StreamState();
    private SyncState joggSyncState = new SyncState();
    private DspState jorbisDspState = new DspState();
    private Block jorbisBlock = new Block(this.jorbisDspState);
    private Comment jorbisComment = new Comment();
    private Info jorbisInfo = new Info();
    private SoundSystemLogger logger = SoundSystemConfig.getLogger();

    public void reverseByteOrder(boolean bl) {
    }

    public boolean initialize(URL uRL) {
        this.initialized(true, false);
        if (this.joggStreamState != null) {
            this.joggStreamState.clear();
        }
        if (this.jorbisBlock != null) {
            this.jorbisBlock.clear();
        }
        if (this.jorbisDspState != null) {
            this.jorbisDspState.clear();
        }
        if (this.jorbisInfo != null) {
            this.jorbisInfo.clear();
        }
        if (this.joggSyncState != null) {
            this.joggSyncState.clear();
        }
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        this.url = uRL;
        this.bufferSize = SoundSystemConfig.getStreamingBufferSize() / 2;
        this.buffer = null;
        this.count = 0;
        this.index = 0;
        this.joggStreamState = new StreamState();
        this.jorbisBlock = new Block(this.jorbisDspState);
        this.jorbisDspState = new DspState();
        this.jorbisInfo = new Info();
        this.joggSyncState = new SyncState();
        try {
            this.urlConnection = uRL.openConnection();
        }
        catch (UnknownServiceException unknownServiceException) {
            this.errorMessage("Unable to create a UrlConnection in method 'initialize'.");
            this.printStackTrace(unknownServiceException);
            this.cleanup();
            return false;
        }
        catch (IOException iOException) {
            this.errorMessage("Unable to create a UrlConnection in method 'initialize'.");
            this.printStackTrace(iOException);
            this.cleanup();
            return false;
        }
        if (this.urlConnection != null) {
            try {
                this.inputStream = this.openInputStream();
            }
            catch (IOException iOException2) {
                this.errorMessage("Unable to acquire inputstream in method 'initialize'.");
                this.printStackTrace(iOException2);
                this.cleanup();
                return false;
            }
        }
        this.endOfStream(true, false);
        this.joggSyncState.init();
        this.joggSyncState.buffer(this.bufferSize);
        this.buffer = this.joggSyncState.data;
        try {
            if (!this.readHeader()) {
                this.errorMessage("Error reading the header");
                return false;
            }
        }
        catch (IOException iOException3) {
            this.errorMessage("Error reading the header");
            return false;
        }
        this.convertedBufferSize = this.bufferSize * 2;
        this.jorbisDspState.synthesis_init(this.jorbisInfo);
        this.jorbisBlock.init(this.jorbisDspState);
        int i = this.jorbisInfo.channels;
        int j = this.jorbisInfo.rate;
        this.audioFormat = new AudioFormat(j, 16, i, true, false);
        this.pcmInfo = new float[1][][];
        this.pcmIndex = new int[this.jorbisInfo.channels];
        this.initialized(true, true);
        return true;
    }

    protected InputStream openInputStream() {
        return this.urlConnection.getInputStream();
    }

    public boolean initialized() {
        return this.initialized(false, false);
    }

    public SoundBuffer read() {
        byte[] bs = this.readBytes();
        if (bs == null) {
            return null;
        }
        return new SoundBuffer(bs, this.audioFormat);
    }

    public SoundBuffer readAll() {
        byte[] bs = this.readBytes();
        while (!(this.endOfStream(false, false) || (bs = CodecJOrbis.appendByteArrays(bs, this.readBytes())) != null && bs.length >= SoundSystemConfig.getMaxFileSize())) {
        }
        return new SoundBuffer(bs, this.audioFormat);
    }

    public boolean endOfStream() {
        return this.endOfStream(false, false);
    }

    public void cleanup() {
        this.joggStreamState.clear();
        this.jorbisBlock.clear();
        this.jorbisDspState.clear();
        this.jorbisInfo.clear();
        this.joggSyncState.clear();
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        this.joggStreamState = null;
        this.jorbisBlock = null;
        this.jorbisDspState = null;
        this.jorbisInfo = null;
        this.joggSyncState = null;
        this.inputStream = null;
    }

    public AudioFormat getAudioFormat() {
        return this.audioFormat;
    }

    private boolean readHeader() {
        this.index = this.joggSyncState.buffer(this.bufferSize);
        int i = this.inputStream.read(this.joggSyncState.data, this.index, this.bufferSize);
        if (i < 0) {
            i = 0;
        }
        this.joggSyncState.wrote(i);
        if (this.joggSyncState.pageout(this.joggPage) != 1) {
            if (i < this.bufferSize) {
                return true;
            }
            this.errorMessage("Ogg header not recognized in method 'readHeader'.");
            return false;
        }
        this.joggStreamState.init(this.joggPage.serialno());
        this.jorbisInfo.init();
        this.jorbisComment.init();
        if (this.joggStreamState.pagein(this.joggPage) < 0) {
            this.errorMessage("Problem with first Ogg header page in method 'readHeader'.");
            return false;
        }
        if (this.joggStreamState.packetout(this.joggPacket) != 1) {
            this.errorMessage("Problem with first Ogg header packet in method 'readHeader'.");
            return false;
        }
        if (this.jorbisInfo.synthesis_headerin(this.jorbisComment, this.joggPacket) < 0) {
            this.errorMessage("File does not contain Vorbis header in method 'readHeader'.");
            return false;
        }
        int j = 0;
        while (j < 2) {
            int k;
            while (j < 2 && (k = this.joggSyncState.pageout(this.joggPage)) != 0) {
                if (k != 1) continue;
                this.joggStreamState.pagein(this.joggPage);
                while (j < 2 && (k = this.joggStreamState.packetout(this.joggPacket)) != 0) {
                    if (k == -1) {
                        this.errorMessage("Secondary Ogg header corrupt in method 'readHeader'.");
                        return false;
                    }
                    this.jorbisInfo.synthesis_headerin(this.jorbisComment, this.joggPacket);
                    ++j;
                }
            }
            this.index = this.joggSyncState.buffer(this.bufferSize);
            i = this.inputStream.read(this.joggSyncState.data, this.index, this.bufferSize);
            if (i < 0) {
                i = 0;
            }
            if (i == 0 && j < 2) {
                this.errorMessage("End of file reached before finished readingOgg header in method 'readHeader'");
                return false;
            }
            this.joggSyncState.wrote(i);
        }
        this.index = this.joggSyncState.buffer(this.bufferSize);
        this.buffer = this.joggSyncState.data;
        return true;
    }

    private byte[] readBytes() {
        byte[] bs;
        if (!this.initialized(false, false)) {
            return null;
        }
        if (this.endOfStream(false, false)) {
            return null;
        }
        Object object = null;
        switch (this.joggSyncState.pageout(this.joggPage)) {
            case -1: 
            case 0: {
                this.endOfStream(true, true);
                break;
            }
            case 1: {
                this.joggStreamState.pagein(this.joggPage);
                if (this.joggPage.granulepos() == 0L) {
                    this.endOfStream(true, true);
                    break;
                }
                block10: while (true) {
                    switch (this.joggStreamState.packetout(this.joggPacket)) {
                        case -1: 
                        case 0: {
                            break block10;
                        }
                        case 1: {
                            bs = CodecJOrbis.appendByteArrays(bs, this.decodeCurrentPacket());
                        }
                        default: {
                            continue block10;
                        }
                    }
                    break;
                }
                if (this.joggPage.eos() == 0) break;
                this.endOfStream(true, true);
            }
        }
        if (!this.endOfStream(false, false)) {
            this.index = this.joggSyncState.buffer(this.bufferSize);
            if (this.index == -1) {
                this.endOfStream(true, true);
            } else {
                this.buffer = this.joggSyncState.data;
                try {
                    this.count = this.inputStream.read(this.buffer, this.index, this.bufferSize);
                }
                catch (Exception exception) {
                    this.printStackTrace(exception);
                    return bs;
                }
                this.joggSyncState.wrote(this.count);
                if (this.count == 0) {
                    this.endOfStream(true, true);
                }
            }
        }
        return bs;
    }

    /*
     * WARNING - void declaration
     */
    private byte[] decodeCurrentPacket() {
        int n;
        int m;
        int k;
        byte[] bs = new byte[this.convertedBufferSize];
        if (this.jorbisBlock.synthesis(this.joggPacket) == 0) {
            this.jorbisDspState.synthesis_blockin(this.jorbisBlock);
        }
        int l = this.convertedBufferSize / (this.jorbisInfo.channels * 2);
        for (m = 0; m < this.convertedBufferSize && (n = this.jorbisDspState.synthesis_pcmout(this.pcmInfo, this.pcmIndex)) > 0; m += k * this.jorbisInfo.channels * 2) {
            void i;
            if (i < l) {
                void var3_5 = i;
            } else {
                k = l;
            }
            for (int n2 = 0; n2 < this.jorbisInfo.channels; ++n2) {
                int o = n2 * 2;
                for (int p = 0; p < k; ++p) {
                    int q = (int)(this.pcmInfo[0][n2][this.pcmIndex[n2] + p] * 32767.0f);
                    if (q > Short.MAX_VALUE) {
                        q = Short.MAX_VALUE;
                    }
                    if (q < Short.MIN_VALUE) {
                        q = Short.MIN_VALUE;
                    }
                    if (q < 0) {
                        q |= 0x8000;
                    }
                    bs[m + o] = (byte)q;
                    bs[m + o + 1] = (byte)(q >>> 8);
                    o += 2 * this.jorbisInfo.channels;
                }
            }
            this.jorbisDspState.synthesis_read(k);
        }
        bs = CodecJOrbis.trimArray(bs, m);
        return bs;
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

    private static byte[] appendByteArrays(byte[] bs, byte[] cs) {
        byte[] fs;
        Object var1_1;
        if (bs == null && cs == null) {
            return null;
        }
        if (bs == null) {
            byte[] ds = new byte[cs.length];
            System.arraycopy(cs, 0, ds, 0, cs.length);
            cs = null;
        } else if (var1_1 == null) {
            byte[] es = new byte[bs.length];
            System.arraycopy(bs, 0, es, 0, bs.length);
            bs = null;
        } else {
            byte[] byArray;
            fs = new byte[byArray.length + ((void)var1_1).length];
            System.arraycopy(byArray, 0, fs, 0, byArray.length);
            System.arraycopy(var1_1, 0, fs, byArray.length, ((void)var1_1).length);
            byArray = null;
            var1_1 = null;
        }
        return fs;
    }

    private void errorMessage(String string) {
        this.logger.errorMessage("CodecJOrbis", string, 0);
    }

    private void printStackTrace(Exception exception) {
        this.logger.printStackTrace(exception, 1);
    }
}

