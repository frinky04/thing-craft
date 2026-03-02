/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jogg;

import com.jcraft.jogg.Page;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class SyncState {
    public byte[] data;
    int storage;
    int fill;
    int returned;
    int unsynced;
    int headerbytes;
    int bodybytes;
    private Page pageseek = new Page();
    private byte[] chksum = new byte[4];

    public int clear() {
        this.data = null;
        return 0;
    }

    public int buffer(int i) {
        if (this.returned != 0) {
            this.fill -= this.returned;
            if (this.fill > 0) {
                System.arraycopy(this.data, this.returned, this.data, 0, this.fill);
            }
            this.returned = 0;
        }
        if (i > this.storage - this.fill) {
            int j = i + this.fill + 4096;
            if (this.data != null) {
                byte[] bs = new byte[j];
                System.arraycopy(this.data, 0, bs, 0, this.data.length);
                this.data = bs;
            } else {
                this.data = new byte[j];
            }
            this.storage = j;
        }
        return this.fill;
    }

    public int wrote(int i) {
        if (this.fill + i > this.storage) {
            return -1;
        }
        this.fill += i;
        return 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public int pageseek(Page page) {
        int i = this.returned;
        int l = this.fill - this.returned;
        if (this.headerbytes == 0) {
            if (l < 27) {
                return 0;
            }
            if (this.data[i] != 79 || this.data[i + 1] != 103 || this.data[i + 2] != 103 || this.data[i + 3] != 83) {
                this.headerbytes = 0;
                this.bodybytes = 0;
                int j = 0;
                for (int o = 0; o < l - 1; ++o) {
                    if (this.data[i + 1 + o] != 79) continue;
                    j = i + 1 + o;
                    break;
                }
                if (j == 0) {
                    j = this.fill;
                }
                this.returned = j;
                return -(j - i);
            }
            int m = (this.data[i + 26] & 0xFF) + 27;
            if (l < m) {
                return 0;
            }
            for (int n = 0; n < (this.data[i + 26] & 0xFF); ++n) {
                this.bodybytes += this.data[i + 27 + n] & 0xFF;
            }
            this.headerbytes = m;
        }
        if (this.bodybytes + this.headerbytes > l) {
            return 0;
        }
        byte[] m = this.chksum;
        synchronized (this.chksum) {
            try {
                System.arraycopy(this.data, i + 22, this.chksum, 0, 4);
                this.data[i + 22] = 0;
                this.data[i + 23] = 0;
                this.data[i + 24] = 0;
                this.data[i + 25] = 0;
                Page page2 = this.pageseek;
                page2.header_base = this.data;
                page2.header = i;
                page2.header_len = this.headerbytes;
                page2.body_base = this.data;
                page2.body = i + this.headerbytes;
                page2.body_len = this.bodybytes;
                page2.checksum();
                if (this.chksum[0] != this.data[i + 22] || this.chksum[1] != this.data[i + 23] || this.chksum[2] != this.data[i + 24] || this.chksum[3] != this.data[i + 25]) {
                    System.arraycopy(this.chksum, 0, this.data, i + 22, 4);
                    this.headerbytes = 0;
                    this.bodybytes = 0;
                    int k = 0;
                    for (int p = 0; p < l - 1; ++p) {
                        if (this.data[i + 1 + p] != 79) continue;
                        k = i + 1 + p;
                        break;
                    }
                    if (k == 0) {
                        k = this.fill;
                    }
                    this.returned = k;
                    // ** MonitorExit[bs] (shouldn't be in output)
                    return -(k - i);
                }
                // ** MonitorExit[bs] (shouldn't be in output)
            }
            catch (Throwable throwable) {
                void throwable2;
                // ** MonitorExit[bs] (shouldn't be in output)
                throw throwable2;
            }
            i = this.returned;
            if (page != null) {
                page.header_base = this.data;
                page.header = i;
                page.header_len = this.headerbytes;
                page.body_base = this.data;
                page.body = i + this.headerbytes;
                page.body_len = this.bodybytes;
            }
            this.unsynced = 0;
            l = this.headerbytes + this.bodybytes;
            this.returned += l;
            this.headerbytes = 0;
            this.bodybytes = 0;
            return l;
        }
    }

    public int pageout(Page page) {
        do {
            int i;
            if ((i = this.pageseek(page)) > 0) {
                return 1;
            }
            if (i != 0) continue;
            return 0;
        } while (this.unsynced != 0);
        this.unsynced = 1;
        return -1;
    }

    public int reset() {
        this.fill = 0;
        this.returned = 0;
        this.unsynced = 0;
        this.headerbytes = 0;
        this.bodybytes = 0;
        return 0;
    }

    public void init() {
    }

    public int getDataOffset() {
        return this.returned;
    }

    public int getBufferOffset() {
        return this.fill;
    }
}

