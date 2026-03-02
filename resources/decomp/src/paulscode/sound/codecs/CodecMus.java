/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package paulscode.sound.codecs;

import java.io.InputStream;
import java.net.URL;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import paulscode.sound.codecs.CodecJOrbis;

@Environment(value=EnvType.CLIENT)
public class CodecMus
extends CodecJOrbis {
    protected InputStream openInputStream() {
        return new MusInputStream(this.url, this.urlConnection.getInputStream());
    }

    @Environment(value=EnvType.CLIENT)
    class MusInputStream
    extends InputStream {
        private int hash;
        private InputStream inputStream;
        byte[] buffer = new byte[1];

        public MusInputStream(URL fileURL, InputStream inputStream) {
            this.inputStream = inputStream;
            String string = fileURL.getPath();
            string = string.substring(string.lastIndexOf("/") + 1);
            this.hash = string.hashCode();
        }

        public int read() {
            int i = this.read(this.buffer, 0, 1);
            if (i < 0) {
                return i;
            }
            return this.buffer[0];
        }

        public int read(byte[] bs, int i, int j) {
            j = this.inputStream.read(bs, i, j);
            for (int k = 0; k < j; ++k) {
                int n = i + k;
                byte by = (byte)(bs[n] ^ this.hash >> 8);
                bs[n] = by;
                byte l = by;
                this.hash = this.hash * 498729871 + 85731 * l;
            }
            return j;
        }
    }
}

