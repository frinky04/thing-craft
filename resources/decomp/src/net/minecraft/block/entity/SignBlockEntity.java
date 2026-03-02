/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;

public class SignBlockEntity
extends BlockEntity {
    public String[] lines = new String[]{"", "", "", ""};
    public int currentRow = -1;

    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Text1", this.lines[0]);
        nbt.putString("Text2", this.lines[1]);
        nbt.putString("Text3", this.lines[2]);
        nbt.putString("Text4", this.lines[3]);
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        for (int i = 0; i < 4; ++i) {
            this.lines[i] = nbt.getString("Text" + (i + 1));
            if (this.lines[i].length() <= 15) continue;
            this.lines[i] = this.lines[i].substring(0, 15);
        }
    }
}

