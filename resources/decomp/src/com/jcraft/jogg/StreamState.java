/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jogg;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class StreamState {
    byte[] body_data;
    int body_storage;
    int body_fill;
    private int body_returned;
    int[] lacing_vals;
    long[] granule_vals;
    int lacing_storage;
    int lacing_fill;
    int lacing_packet;
    int lacing_returned;
    byte[] header = new byte[282];
    int header_fill;
    public int e_o_s;
    int b_o_s;
    int serialno;
    int pageno;
    long packetno;
    long granulepos;

    public StreamState() {
        this.init();
    }

    StreamState(int i) {
        this();
        this.init(i);
    }

    void init() {
        this.body_storage = 16384;
        this.body_data = new byte[this.body_storage];
        this.lacing_storage = 1024;
        this.lacing_vals = new int[this.lacing_storage];
        this.granule_vals = new long[this.lacing_storage];
    }

    public void init(int i) {
        if (this.body_data == null) {
            this.init();
        } else {
            int j;
            for (j = 0; j < this.body_data.length; ++j) {
                this.body_data[j] = 0;
            }
            for (j = 0; j < this.lacing_vals.length; ++j) {
                this.lacing_vals[j] = 0;
            }
            for (j = 0; j < this.granule_vals.length; ++j) {
                this.granule_vals[j] = 0L;
            }
        }
        this.serialno = i;
    }

    public void clear() {
        this.body_data = null;
        this.lacing_vals = null;
        this.granule_vals = null;
    }

    void destroy() {
        this.clear();
    }

    void body_expand(int i) {
        if (this.body_storage <= this.body_fill + i) {
            this.body_storage += i + 1024;
            byte[] bs = new byte[this.body_storage];
            System.arraycopy(this.body_data, 0, bs, 0, this.body_data.length);
            this.body_data = bs;
        }
    }

    void lacing_expand(int i) {
        if (this.lacing_storage <= this.lacing_fill + i) {
            this.lacing_storage += i + 32;
            int[] is = new int[this.lacing_storage];
            System.arraycopy(this.lacing_vals, 0, is, 0, this.lacing_vals.length);
            this.lacing_vals = is;
            long[] ls = new long[this.lacing_storage];
            System.arraycopy(this.granule_vals, 0, ls, 0, this.granule_vals.length);
            this.granule_vals = ls;
        }
    }

    public int packetin(Packet packet) {
        int i = packet.bytes / 255 + 1;
        if (this.body_returned != 0) {
            this.body_fill -= this.body_returned;
            if (this.body_fill != 0) {
                System.arraycopy(this.body_data, this.body_returned, this.body_data, 0, this.body_fill);
            }
            this.body_returned = 0;
        }
        this.body_expand(packet.bytes);
        this.lacing_expand(i);
        System.arraycopy(packet.packet_base, packet.packet, this.body_data, this.body_fill, packet.bytes);
        this.body_fill += packet.bytes;
        for (int j = 0; j < i - 1; ++j) {
            this.lacing_vals[this.lacing_fill + j] = 255;
            this.granule_vals[this.lacing_fill + j] = this.granulepos;
        }
        this.lacing_vals[this.lacing_fill + j] = packet.bytes % 255;
        long l = packet.granulepos;
        this.granule_vals[this.lacing_fill + j] = l;
        this.granulepos = l;
        int n = this.lacing_fill;
        this.lacing_vals[n] = this.lacing_vals[n] | 0x100;
        this.lacing_fill += i;
        ++this.packetno;
        if (packet.e_o_s != 0) {
            this.e_o_s = 1;
        }
        return 0;
    }

    public int packetout(Packet packet) {
        int i;
        if (this.lacing_packet <= (i = this.lacing_returned++)) {
            return 0;
        }
        if ((this.lacing_vals[i] & 0x400) != 0) {
            ++this.packetno;
            return -1;
        }
        int j = this.lacing_vals[i] & 0xFF;
        int k = 0;
        packet.packet_base = this.body_data;
        packet.packet = this.body_returned;
        packet.e_o_s = this.lacing_vals[i] & 0x200;
        packet.b_o_s = this.lacing_vals[i] & 0x100;
        k += j;
        while (j == 255) {
            int l = this.lacing_vals[++i];
            j = l & 0xFF;
            if ((l & 0x200) != 0) {
                packet.e_o_s = 512;
            }
            k += j;
        }
        packet.packetno = this.packetno++;
        packet.granulepos = this.granule_vals[i];
        packet.bytes = k;
        this.body_returned += k;
        this.lacing_returned = i + 1;
        return 1;
    }

    public int pagein(Page page) {
        byte[] bs = page.header_base;
        int i = page.header;
        byte[] cs = page.body_base;
        int j = page.body;
        int k = page.body_len;
        int l = 0;
        int m = page.version();
        int n = page.continued();
        int o = page.bos();
        int p = page.eos();
        long q = page.granulepos();
        int r = page.serialno();
        int s = page.pageno();
        int t = bs[i + 26] & 0xFF;
        int u = this.lacing_returned;
        int v = this.body_returned;
        if (v != 0) {
            this.body_fill -= v;
            if (this.body_fill != 0) {
                System.arraycopy(this.body_data, v, this.body_data, 0, this.body_fill);
            }
            this.body_returned = 0;
        }
        if (u != 0) {
            if (this.lacing_fill - u != 0) {
                System.arraycopy(this.lacing_vals, u, this.lacing_vals, 0, this.lacing_fill - u);
                System.arraycopy(this.granule_vals, u, this.granule_vals, 0, this.lacing_fill - u);
            }
            this.lacing_fill -= u;
            this.lacing_packet -= u;
            this.lacing_returned = 0;
        }
        if (r != this.serialno) {
            return -1;
        }
        if (m > 0) {
            return -1;
        }
        this.lacing_expand(t + 1);
        if (s != this.pageno) {
            for (u = this.lacing_packet; u < this.lacing_fill; ++u) {
                this.body_fill -= this.lacing_vals[u] & 0xFF;
            }
            this.lacing_fill = this.lacing_packet++;
            if (this.pageno != -1) {
                this.lacing_vals[this.lacing_fill++] = 1024;
            }
            if (n != 0) {
                o = 0;
                while (l < t) {
                    v = bs[i + 27 + l] & 0xFF;
                    j += v;
                    k -= v;
                    if (v < 255) {
                        ++l;
                        break;
                    }
                    ++l;
                }
            }
        }
        if (k != 0) {
            this.body_expand(k);
            System.arraycopy(cs, j, this.body_data, this.body_fill, k);
            this.body_fill += k;
        }
        u = -1;
        while (l < t) {
            this.lacing_vals[this.lacing_fill] = v = bs[i + 27 + l] & 0xFF;
            this.granule_vals[this.lacing_fill] = -1L;
            if (o != 0) {
                int n2 = this.lacing_fill;
                this.lacing_vals[n2] = this.lacing_vals[n2] | 0x100;
                o = 0;
            }
            if (v < 255) {
                u = this.lacing_fill;
            }
            ++this.lacing_fill;
            ++l;
            if (v >= 255) continue;
            this.lacing_packet = this.lacing_fill;
        }
        if (u != -1) {
            this.granule_vals[u] = q;
        }
        if (p != 0) {
            this.e_o_s = 1;
            if (this.lacing_fill > 0) {
                int n3 = this.lacing_fill - 1;
                this.lacing_vals[n3] = this.lacing_vals[n3] | 0x200;
            }
        }
        this.pageno = s + 1;
        return 0;
    }

    public int flush(Page page) {
        int i;
        int j = 0;
        int k = this.lacing_fill > 255 ? 255 : this.lacing_fill;
        int l = 0;
        int m = 0;
        long n = this.granule_vals[0];
        if (k == 0) {
            return 0;
        }
        if (this.b_o_s == 0) {
            n = 0L;
            for (j = 0; j < k; ++j) {
                if ((this.lacing_vals[j] & 0xFF) >= 255) continue;
                ++j;
                break;
            }
        } else {
            for (j = 0; j < k && m <= 4096; m += this.lacing_vals[j] & 0xFF, ++j) {
                n = this.granule_vals[j];
            }
        }
        System.arraycopy("OggS".getBytes(), 0, this.header, 0, 4);
        this.header[4] = 0;
        this.header[5] = 0;
        if ((this.lacing_vals[0] & 0x100) == 0) {
            this.header[5] = (byte)(this.header[5] | 1);
        }
        if (this.b_o_s == 0) {
            this.header[5] = (byte)(this.header[5] | 2);
        }
        if (this.e_o_s != 0 && this.lacing_fill == j) {
            this.header[5] = (byte)(this.header[5] | 4);
        }
        this.b_o_s = 1;
        for (i = 6; i < 14; ++i) {
            this.header[i] = (byte)n;
            n >>>= 8;
        }
        int o = this.serialno;
        for (i = 14; i < 18; ++i) {
            this.header[i] = (byte)o;
            o >>>= 8;
        }
        if (this.pageno == -1) {
            this.pageno = 0;
        }
        o = this.pageno++;
        for (i = 18; i < 22; ++i) {
            this.header[i] = (byte)o;
            o >>>= 8;
        }
        this.header[22] = 0;
        this.header[23] = 0;
        this.header[24] = 0;
        this.header[25] = 0;
        this.header[26] = (byte)j;
        for (i = 0; i < j; ++i) {
            this.header[i + 27] = (byte)this.lacing_vals[i];
            l += this.header[i + 27] & 0xFF;
        }
        page.header_base = this.header;
        page.header = 0;
        page.header_len = this.header_fill = j + 27;
        page.body_base = this.body_data;
        page.body = this.body_returned;
        page.body_len = l;
        this.lacing_fill -= j;
        System.arraycopy(this.lacing_vals, j, this.lacing_vals, 0, this.lacing_fill * 4);
        System.arraycopy(this.granule_vals, j, this.granule_vals, 0, this.lacing_fill * 8);
        this.body_returned += l;
        page.checksum();
        return 1;
    }

    public int pageout(Page page) {
        if (this.e_o_s != 0 && this.lacing_fill != 0 || this.body_fill - this.body_returned > 4096 || this.lacing_fill >= 255 || this.lacing_fill != 0 && this.b_o_s == 0) {
            return this.flush(page);
        }
        return 0;
    }

    public int eof() {
        return this.e_o_s;
    }

    public int reset() {
        this.body_fill = 0;
        this.body_returned = 0;
        this.lacing_fill = 0;
        this.lacing_packet = 0;
        this.lacing_returned = 0;
        this.header_fill = 0;
        this.e_o_s = 0;
        this.b_o_s = 0;
        this.pageno = -1;
        this.packetno = 0L;
        this.granulepos = 0L;
        return 0;
    }
}

