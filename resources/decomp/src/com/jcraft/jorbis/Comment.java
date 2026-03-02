/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import com.jcraft.jogg.Buffer;
import com.jcraft.jogg.Packet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class Comment {
    private static byte[] _vorbis = "vorbis".getBytes();
    private static byte[] _vendor = "Xiphophorus libVorbis I 20000508".getBytes();
    private static final int OV_EIMPL = -130;
    public byte[][] user_comments;
    public int[] comment_lengths;
    public int comments;
    public byte[] vendor;

    public void init() {
        this.user_comments = null;
        this.comments = 0;
        this.vendor = null;
    }

    public void add(String string) {
        this.add(string.getBytes());
    }

    private void add(byte[] bs) {
        byte[][] cs = new byte[this.comments + 2][];
        if (this.user_comments != null) {
            System.arraycopy(this.user_comments, 0, cs, 0, this.comments);
        }
        this.user_comments = cs;
        int[] is = new int[this.comments + 2];
        if (this.comment_lengths != null) {
            System.arraycopy(this.comment_lengths, 0, is, 0, this.comments);
        }
        this.comment_lengths = is;
        byte[] ds = new byte[bs.length + 1];
        System.arraycopy(bs, 0, ds, 0, bs.length);
        this.user_comments[this.comments] = ds;
        this.comment_lengths[this.comments] = bs.length;
        ++this.comments;
        this.user_comments[this.comments] = null;
    }

    public void add_tag(String string, String string2) {
        if (string2 == null) {
            string2 = "";
        }
        this.add(string + "=" + string2);
    }

    static boolean tagcompare(byte[] bs, byte[] cs, int i) {
        for (int j = 0; j < i; ++j) {
            byte k = bs[j];
            byte l = cs[j];
            if (90 >= k && k >= 65) {
                k = (byte)(k - 65 + 97);
            }
            if (90 >= l && l >= 65) {
                l = (byte)(l - 65 + 97);
            }
            if (k == l) continue;
            return false;
        }
        return true;
    }

    public String query(String string) {
        return this.query(string, 0);
    }

    public String query(String string, int i) {
        int j = this.query(string.getBytes(), i);
        if (j == -1) {
            return null;
        }
        byte[] bs = this.user_comments[j];
        for (int k = 0; k < this.comment_lengths[j]; ++k) {
            if (bs[k] != 61) continue;
            return new String(bs, k + 1, this.comment_lengths[j] - (k + 1));
        }
        return null;
    }

    private int query(byte[] bs, int i) {
        int j = 0;
        int k = 0;
        int l = bs.length + 1;
        byte[] cs = new byte[l];
        System.arraycopy(bs, 0, cs, 0, bs.length);
        cs[bs.length] = 61;
        for (j = 0; j < this.comments; ++j) {
            if (!Comment.tagcompare(this.user_comments[j], cs, l)) continue;
            if (i == k) {
                return j;
            }
            ++k;
        }
        return -1;
    }

    int unpack(Buffer buffer) {
        int i = buffer.read(32);
        if (i < 0) {
            this.clear();
            return -1;
        }
        this.vendor = new byte[i + 1];
        buffer.read(this.vendor, i);
        this.comments = buffer.read(32);
        if (this.comments < 0) {
            this.clear();
            return -1;
        }
        this.user_comments = new byte[this.comments + 1][];
        this.comment_lengths = new int[this.comments + 1];
        for (int j = 0; j < this.comments; ++j) {
            int k = buffer.read(32);
            if (k < 0) {
                this.clear();
                return -1;
            }
            this.comment_lengths[j] = k;
            this.user_comments[j] = new byte[k + 1];
            buffer.read(this.user_comments[j], k);
        }
        if (buffer.read(1) != 1) {
            this.clear();
            return -1;
        }
        return 0;
    }

    int pack(Buffer buffer) {
        buffer.write(3, 8);
        buffer.write(_vorbis);
        buffer.write(_vendor.length, 32);
        buffer.write(_vendor);
        buffer.write(this.comments, 32);
        if (this.comments != 0) {
            for (int i = 0; i < this.comments; ++i) {
                if (this.user_comments[i] != null) {
                    buffer.write(this.comment_lengths[i], 32);
                    buffer.write(this.user_comments[i]);
                    continue;
                }
                buffer.write(0, 32);
            }
        }
        buffer.write(1, 1);
        return 0;
    }

    public int header_out(Packet packet) {
        Buffer buffer = new Buffer();
        buffer.writeinit();
        if (this.pack(buffer) != 0) {
            return -130;
        }
        packet.packet_base = new byte[buffer.bytes()];
        packet.packet = 0;
        packet.bytes = buffer.bytes();
        System.arraycopy(buffer.buffer(), 0, packet.packet_base, 0, packet.bytes);
        packet.b_o_s = 0;
        packet.e_o_s = 0;
        packet.granulepos = 0L;
        return 0;
    }

    void clear() {
        for (int i = 0; i < this.comments; ++i) {
            this.user_comments[i] = null;
        }
        this.user_comments = null;
        this.vendor = null;
    }

    public String getVendor() {
        return new String(this.vendor, 0, this.vendor.length - 1);
    }

    public String getComment(int i) {
        if (this.comments <= i) {
            return null;
        }
        return new String(this.user_comments[i], 0, this.user_comments[i].length - 1);
    }

    public String toString() {
        String string = "Vendor: " + new String(this.vendor, 0, this.vendor.length - 1);
        for (int i = 0; i < this.comments; ++i) {
            string = string + "\nComment: " + new String(this.user_comments[i], 0, this.user_comments[i].length - 1);
        }
        string = string + "\n";
        return string;
    }
}

