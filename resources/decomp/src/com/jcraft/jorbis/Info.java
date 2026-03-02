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
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.FuncFloor;
import com.jcraft.jorbis.FuncMapping;
import com.jcraft.jorbis.FuncResidue;
import com.jcraft.jorbis.FuncTime;
import com.jcraft.jorbis.InfoMode;
import com.jcraft.jorbis.PsyInfo;
import com.jcraft.jorbis.StaticCodeBook;
import com.jcraft.jorbis.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class Info {
    private static final int OV_EBADPACKET = -136;
    private static final int OV_ENOTAUDIO = -135;
    private static byte[] _vorbis = "vorbis".getBytes();
    private static final int VI_TIMEB = 1;
    private static final int VI_FLOORB = 2;
    private static final int VI_RESB = 3;
    private static final int VI_MAPB = 1;
    private static final int VI_WINDOWB = 1;
    public int version;
    public int channels;
    public int rate;
    int bitrate_upper;
    int bitrate_nominal;
    int bitrate_lower;
    int[] blocksizes = new int[2];
    int modes;
    int maps;
    int times;
    int floors;
    int residues;
    int books;
    int psys;
    InfoMode[] mode_param = null;
    int[] map_type = null;
    Object[] map_param = null;
    int[] time_type = null;
    Object[] time_param = null;
    int[] floor_type = null;
    Object[] floor_param = null;
    int[] residue_type = null;
    Object[] residue_param = null;
    StaticCodeBook[] book_param = null;
    PsyInfo[] psy_param = new PsyInfo[64];
    int envelopesa;
    float preecho_thresh;
    float preecho_clamp;

    public void init() {
        this.rate = 0;
    }

    public void clear() {
        int i;
        for (i = 0; i < this.modes; ++i) {
            this.mode_param[i] = null;
        }
        this.mode_param = null;
        for (i = 0; i < this.maps; ++i) {
            FuncMapping.mapping_P[this.map_type[i]].free_info(this.map_param[i]);
        }
        this.map_param = null;
        for (i = 0; i < this.times; ++i) {
            FuncTime.time_P[this.time_type[i]].free_info(this.time_param[i]);
        }
        this.time_param = null;
        for (i = 0; i < this.floors; ++i) {
            FuncFloor.floor_P[this.floor_type[i]].free_info(this.floor_param[i]);
        }
        this.floor_param = null;
        for (i = 0; i < this.residues; ++i) {
            FuncResidue.residue_P[this.residue_type[i]].free_info(this.residue_param[i]);
        }
        this.residue_param = null;
        for (i = 0; i < this.books; ++i) {
            if (this.book_param[i] == null) continue;
            this.book_param[i].clear();
            this.book_param[i] = null;
        }
        this.book_param = null;
        for (i = 0; i < this.psys; ++i) {
            this.psy_param[i].free();
        }
    }

    int unpack_info(Buffer buffer) {
        this.version = buffer.read(32);
        if (this.version != 0) {
            return -1;
        }
        this.channels = buffer.read(8);
        this.rate = buffer.read(32);
        this.bitrate_upper = buffer.read(32);
        this.bitrate_nominal = buffer.read(32);
        this.bitrate_lower = buffer.read(32);
        this.blocksizes[0] = 1 << buffer.read(4);
        this.blocksizes[1] = 1 << buffer.read(4);
        if (this.rate < 1 || this.channels < 1 || this.blocksizes[0] < 8 || this.blocksizes[1] < this.blocksizes[0] || buffer.read(1) != 1) {
            this.clear();
            return -1;
        }
        return 0;
    }

    int unpack_books(Buffer buffer) {
        int i;
        this.books = buffer.read(8) + 1;
        if (this.book_param == null || this.book_param.length != this.books) {
            this.book_param = new StaticCodeBook[this.books];
        }
        for (i = 0; i < this.books; ++i) {
            this.book_param[i] = new StaticCodeBook();
            if (this.book_param[i].unpack(buffer) == 0) continue;
            this.clear();
            return -1;
        }
        this.times = buffer.read(6) + 1;
        if (this.time_type == null || this.time_type.length != this.times) {
            this.time_type = new int[this.times];
        }
        if (this.time_param == null || this.time_param.length != this.times) {
            this.time_param = new Object[this.times];
        }
        for (i = 0; i < this.times; ++i) {
            this.time_type[i] = buffer.read(16);
            if (this.time_type[i] < 0 || this.time_type[i] >= 1) {
                this.clear();
                return -1;
            }
            this.time_param[i] = FuncTime.time_P[this.time_type[i]].unpack(this, buffer);
            if (this.time_param[i] != null) continue;
            this.clear();
            return -1;
        }
        this.floors = buffer.read(6) + 1;
        if (this.floor_type == null || this.floor_type.length != this.floors) {
            this.floor_type = new int[this.floors];
        }
        if (this.floor_param == null || this.floor_param.length != this.floors) {
            this.floor_param = new Object[this.floors];
        }
        for (i = 0; i < this.floors; ++i) {
            this.floor_type[i] = buffer.read(16);
            if (this.floor_type[i] < 0 || this.floor_type[i] >= 2) {
                this.clear();
                return -1;
            }
            this.floor_param[i] = FuncFloor.floor_P[this.floor_type[i]].unpack(this, buffer);
            if (this.floor_param[i] != null) continue;
            this.clear();
            return -1;
        }
        this.residues = buffer.read(6) + 1;
        if (this.residue_type == null || this.residue_type.length != this.residues) {
            this.residue_type = new int[this.residues];
        }
        if (this.residue_param == null || this.residue_param.length != this.residues) {
            this.residue_param = new Object[this.residues];
        }
        for (i = 0; i < this.residues; ++i) {
            this.residue_type[i] = buffer.read(16);
            if (this.residue_type[i] < 0 || this.residue_type[i] >= 3) {
                this.clear();
                return -1;
            }
            this.residue_param[i] = FuncResidue.residue_P[this.residue_type[i]].unpack(this, buffer);
            if (this.residue_param[i] != null) continue;
            this.clear();
            return -1;
        }
        this.maps = buffer.read(6) + 1;
        if (this.map_type == null || this.map_type.length != this.maps) {
            this.map_type = new int[this.maps];
        }
        if (this.map_param == null || this.map_param.length != this.maps) {
            this.map_param = new Object[this.maps];
        }
        for (i = 0; i < this.maps; ++i) {
            this.map_type[i] = buffer.read(16);
            if (this.map_type[i] < 0 || this.map_type[i] >= 1) {
                this.clear();
                return -1;
            }
            this.map_param[i] = FuncMapping.mapping_P[this.map_type[i]].unpack(this, buffer);
            if (this.map_param[i] != null) continue;
            this.clear();
            return -1;
        }
        this.modes = buffer.read(6) + 1;
        if (this.mode_param == null || this.mode_param.length != this.modes) {
            this.mode_param = new InfoMode[this.modes];
        }
        for (i = 0; i < this.modes; ++i) {
            this.mode_param[i] = new InfoMode();
            this.mode_param[i].blockflag = buffer.read(1);
            this.mode_param[i].windowtype = buffer.read(16);
            this.mode_param[i].transformtype = buffer.read(16);
            this.mode_param[i].mapping = buffer.read(8);
            if (this.mode_param[i].windowtype < 1 && this.mode_param[i].transformtype < 1 && this.mode_param[i].mapping < this.maps) continue;
            this.clear();
            return -1;
        }
        if (buffer.read(1) != 1) {
            this.clear();
            return -1;
        }
        return 0;
    }

    public int synthesis_headerin(Comment comment, Packet packet) {
        Buffer buffer = new Buffer();
        if (packet != null) {
            buffer.readinit(packet.packet_base, packet.packet, packet.bytes);
            byte[] bs = new byte[6];
            int i = buffer.read(8);
            buffer.read(bs, 6);
            if (bs[0] != 118 || bs[1] != 111 || bs[2] != 114 || bs[3] != 98 || bs[4] != 105 || bs[5] != 115) {
                return -1;
            }
            switch (i) {
                case 1: {
                    if (packet.b_o_s == 0) {
                        return -1;
                    }
                    if (this.rate != 0) {
                        return -1;
                    }
                    return this.unpack_info(buffer);
                }
                case 3: {
                    if (this.rate == 0) {
                        return -1;
                    }
                    return comment.unpack(buffer);
                }
                case 5: {
                    if (this.rate == 0 || comment.vendor == null) {
                        return -1;
                    }
                    return this.unpack_books(buffer);
                }
            }
        }
        return -1;
    }

    int pack_info(Buffer buffer) {
        buffer.write(1, 8);
        buffer.write(_vorbis);
        buffer.write(0, 32);
        buffer.write(this.channels, 8);
        buffer.write(this.rate, 32);
        buffer.write(this.bitrate_upper, 32);
        buffer.write(this.bitrate_nominal, 32);
        buffer.write(this.bitrate_lower, 32);
        buffer.write(Util.ilog2(this.blocksizes[0]), 4);
        buffer.write(Util.ilog2(this.blocksizes[1]), 4);
        buffer.write(1, 1);
        return 0;
    }

    int pack_books(Buffer buffer) {
        int i;
        buffer.write(5, 8);
        buffer.write(_vorbis);
        buffer.write(this.books - 1, 8);
        for (i = 0; i < this.books; ++i) {
            if (this.book_param[i].pack(buffer) == 0) continue;
            return -1;
        }
        buffer.write(this.times - 1, 6);
        for (i = 0; i < this.times; ++i) {
            buffer.write(this.time_type[i], 16);
            FuncTime.time_P[this.time_type[i]].pack(this.time_param[i], buffer);
        }
        buffer.write(this.floors - 1, 6);
        for (i = 0; i < this.floors; ++i) {
            buffer.write(this.floor_type[i], 16);
            FuncFloor.floor_P[this.floor_type[i]].pack(this.floor_param[i], buffer);
        }
        buffer.write(this.residues - 1, 6);
        for (i = 0; i < this.residues; ++i) {
            buffer.write(this.residue_type[i], 16);
            FuncResidue.residue_P[this.residue_type[i]].pack(this.residue_param[i], buffer);
        }
        buffer.write(this.maps - 1, 6);
        for (i = 0; i < this.maps; ++i) {
            buffer.write(this.map_type[i], 16);
            FuncMapping.mapping_P[this.map_type[i]].pack(this, this.map_param[i], buffer);
        }
        buffer.write(this.modes - 1, 6);
        for (i = 0; i < this.modes; ++i) {
            buffer.write(this.mode_param[i].blockflag, 1);
            buffer.write(this.mode_param[i].windowtype, 16);
            buffer.write(this.mode_param[i].transformtype, 16);
            buffer.write(this.mode_param[i].mapping, 8);
        }
        buffer.write(1, 1);
        return 0;
    }

    public int blocksize(Packet packet) {
        Buffer buffer = new Buffer();
        buffer.readinit(packet.packet_base, packet.packet, packet.bytes);
        if (buffer.read(1) != 0) {
            return -135;
        }
        int j = 0;
        for (int k = this.modes; k > 1; k >>>= 1) {
            ++j;
        }
        int i = buffer.read(j);
        if (i == -1) {
            return -136;
        }
        return this.blocksizes[this.mode_param[i].blockflag];
    }

    public String toString() {
        return "version:" + new Integer(this.version) + ", channels:" + new Integer(this.channels) + ", rate:" + new Integer(this.rate) + ", bitrate:" + new Integer(this.bitrate_upper) + "," + new Integer(this.bitrate_nominal) + "," + new Integer(this.bitrate_lower);
    }
}

