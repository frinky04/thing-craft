/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class RailBlock
extends Block {
    protected RailBlock(int id, int sprite) {
        super(id, sprite, Material.DECORATION);
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.125f, 1.0f);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return null;
    }

    public boolean isSolid() {
        return false;
    }

    public HitResult rayTrace(World world, int x, int y, int z, Vec3d from, Vec3d to) {
        this.updateShape((WorldView)world, x, y, z);
        return super.rayTrace(world, x, y, z, from, to);
    }

    public void updateShape(WorldView world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        if (i >= 2 && i <= 5) {
            this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.625f, 1.0f);
        } else {
            this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.125f, 1.0f);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        if (metadata >= 6) {
            return this.sprite - 16;
        }
        return this.sprite;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return 9;
    }

    public int getBaseDropCount(Random random) {
        return 1;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        return world.isSolidBlock(x, y - 1, z);
    }

    public void onAdded(World world, int x, int y, int z) {
        if (!world.isMultiplayer) {
            world.setBlockMetadata(x, y, z, 15);
            this.updateShape(world, x, y, z);
        }
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (world.isMultiplayer) {
            return;
        }
        int i = world.getBlockMetadata(x, y, z);
        boolean j = false;
        if (!world.isSolidBlock(x, y - 1, z)) {
            j = true;
        }
        if (i == 2 && !world.isSolidBlock(x + 1, y, z)) {
            j = true;
        }
        if (i == 3 && !world.isSolidBlock(x - 1, y, z)) {
            j = true;
        }
        if (i == 4 && !world.isSolidBlock(x, y, z - 1)) {
            j = true;
        }
        if (i == 5 && !world.isSolidBlock(x, y, z + 1)) {
            j = true;
        }
        if (j) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
        } else if (neighborBlock > 0 && Block.BY_ID[neighborBlock].isSignalSource() && new RailNode(world, x, y, z).countConnections() == 3) {
            this.updateShape(world, x, y, z);
        }
    }

    private void updateShape(World world, int x, int y, int z) {
        if (world.isMultiplayer) {
            return;
        }
        new RailNode(world, x, y, z).updateState(world.hasNeighborSignal(x, y, z));
    }

    public class RailNode {
        private World world;
        private int x;
        private int y;
        private int z;
        private int metadata;
        private List connections = new ArrayList();

        public RailNode(World world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.metadata = world.getBlockMetadata(x, y, z);
            this.updateConnections();
        }

        private void updateConnections() {
            this.connections.clear();
            if (this.metadata == 0) {
                this.connections.add(new BlockPos(this.x, this.y, this.z - 1));
                this.connections.add(new BlockPos(this.x, this.y, this.z + 1));
            } else if (this.metadata == 1) {
                this.connections.add(new BlockPos(this.x - 1, this.y, this.z));
                this.connections.add(new BlockPos(this.x + 1, this.y, this.z));
            } else if (this.metadata == 2) {
                this.connections.add(new BlockPos(this.x - 1, this.y, this.z));
                this.connections.add(new BlockPos(this.x + 1, this.y + 1, this.z));
            } else if (this.metadata == 3) {
                this.connections.add(new BlockPos(this.x - 1, this.y + 1, this.z));
                this.connections.add(new BlockPos(this.x + 1, this.y, this.z));
            } else if (this.metadata == 4) {
                this.connections.add(new BlockPos(this.x, this.y + 1, this.z - 1));
                this.connections.add(new BlockPos(this.x, this.y, this.z + 1));
            } else if (this.metadata == 5) {
                this.connections.add(new BlockPos(this.x, this.y, this.z - 1));
                this.connections.add(new BlockPos(this.x, this.y + 1, this.z + 1));
            } else if (this.metadata == 6) {
                this.connections.add(new BlockPos(this.x + 1, this.y, this.z));
                this.connections.add(new BlockPos(this.x, this.y, this.z + 1));
            } else if (this.metadata == 7) {
                this.connections.add(new BlockPos(this.x - 1, this.y, this.z));
                this.connections.add(new BlockPos(this.x, this.y, this.z + 1));
            } else if (this.metadata == 8) {
                this.connections.add(new BlockPos(this.x - 1, this.y, this.z));
                this.connections.add(new BlockPos(this.x, this.y, this.z - 1));
            } else if (this.metadata == 9) {
                this.connections.add(new BlockPos(this.x + 1, this.y, this.z));
                this.connections.add(new BlockPos(this.x, this.y, this.z - 1));
            }
        }

        private void removeSoftConnections() {
            for (int i = 0; i < this.connections.size(); ++i) {
                RailNode railNode = this.getNeighborRail((BlockPos)this.connections.get(i));
                if (railNode == null || !railNode.connectsTo(this)) {
                    this.connections.remove(i--);
                    continue;
                }
                this.connections.set(i, new BlockPos(railNode.x, railNode.y, railNode.z));
            }
        }

        private boolean couldConnectTo(int x, int y, int z) {
            if (this.world.getBlock(x, y, z) == RailBlock.this.id) {
                return true;
            }
            if (this.world.getBlock(x, y + 1, z) == RailBlock.this.id) {
                return true;
            }
            return this.world.getBlock(x, y - 1, z) == RailBlock.this.id;
        }

        private RailNode getNeighborRail(BlockPos pos) {
            if (this.world.getBlock(pos.x, pos.y, pos.z) == RailBlock.this.id) {
                return new RailNode(this.world, pos.x, pos.y, pos.z);
            }
            if (this.world.getBlock(pos.x, pos.y + 1, pos.z) == RailBlock.this.id) {
                return new RailNode(this.world, pos.x, pos.y + 1, pos.z);
            }
            if (this.world.getBlock(pos.x, pos.y - 1, pos.z) == RailBlock.this.id) {
                return new RailNode(this.world, pos.x, pos.y - 1, pos.z);
            }
            return null;
        }

        private boolean connectsTo(RailNode rail) {
            for (int i = 0; i < this.connections.size(); ++i) {
                BlockPos blockPos = (BlockPos)this.connections.get(i);
                if (blockPos.x != rail.x || blockPos.z != rail.z) continue;
                return true;
            }
            return false;
        }

        private boolean hasConnection(int x, int y, int z) {
            for (int i = 0; i < this.connections.size(); ++i) {
                BlockPos blockPos = (BlockPos)this.connections.get(i);
                if (blockPos.x != x || blockPos.z != z) continue;
                return true;
            }
            return false;
        }

        private int countConnections() {
            int i = 0;
            if (this.couldConnectTo(this.x, this.y, this.z - 1)) {
                ++i;
            }
            if (this.couldConnectTo(this.x, this.y, this.z + 1)) {
                ++i;
            }
            if (this.couldConnectTo(this.x - 1, this.y, this.z)) {
                ++i;
            }
            if (this.couldConnectTo(this.x + 1, this.y, this.z)) {
                ++i;
            }
            return i;
        }

        private boolean canConnectTo(RailNode rail) {
            if (this.connectsTo(rail)) {
                return true;
            }
            if (this.connections.size() == 2) {
                return false;
            }
            if (this.connections.size() == 0) {
                return true;
            }
            BlockPos blockPos = (BlockPos)this.connections.get(0);
            if (rail.y == this.y && blockPos.y == this.y) {
                return true;
            }
            return true;
        }

        private void addConnection(RailNode rail) {
            this.connections.add(new BlockPos(rail.x, rail.y, rail.z));
            boolean i = this.hasConnection(this.x, this.y, this.z - 1);
            boolean j = this.hasConnection(this.x, this.y, this.z + 1);
            boolean k = this.hasConnection(this.x - 1, this.y, this.z);
            boolean l = this.hasConnection(this.x + 1, this.y, this.z);
            int m = -1;
            if (i || j) {
                m = 0;
            }
            if (k || l) {
                m = 1;
            }
            if (j && l && !i && !k) {
                m = 6;
            }
            if (j && k && !i && !l) {
                m = 7;
            }
            if (i && k && !j && !l) {
                m = 8;
            }
            if (i && l && !j && !k) {
                m = 9;
            }
            if (m == 0) {
                if (this.world.getBlock(this.x, this.y + 1, this.z - 1) == RailBlock.this.id) {
                    m = 4;
                }
                if (this.world.getBlock(this.x, this.y + 1, this.z + 1) == RailBlock.this.id) {
                    m = 5;
                }
            }
            if (m == 1) {
                if (this.world.getBlock(this.x + 1, this.y + 1, this.z) == RailBlock.this.id) {
                    m = 2;
                }
                if (this.world.getBlock(this.x - 1, this.y + 1, this.z) == RailBlock.this.id) {
                    m = 3;
                }
            }
            if (m < 0) {
                m = 0;
            }
            this.world.setBlockMetadata(this.x, this.y, this.z, m);
        }

        private boolean hasNeighborRail(int x, int y, int z) {
            RailNode railNode = this.getNeighborRail(new BlockPos(x, y, z));
            if (railNode == null) {
                return false;
            }
            railNode.removeSoftConnections();
            return railNode.canConnectTo(this);
        }

        public void updateState(boolean powered) {
            boolean i = this.hasNeighborRail(this.x, this.y, this.z - 1);
            boolean j = this.hasNeighborRail(this.x, this.y, this.z + 1);
            boolean k = this.hasNeighborRail(this.x - 1, this.y, this.z);
            boolean l = this.hasNeighborRail(this.x + 1, this.y, this.z);
            int m = -1;
            if ((i || j) && !k && !l) {
                m = 0;
            }
            if ((k || l) && !i && !j) {
                m = 1;
            }
            if (j && l && !i && !k) {
                m = 6;
            }
            if (j && k && !i && !l) {
                m = 7;
            }
            if (i && k && !j && !l) {
                m = 8;
            }
            if (i && l && !j && !k) {
                m = 9;
            }
            if (m == -1) {
                if (i || j) {
                    m = 0;
                }
                if (k || l) {
                    m = 1;
                }
                if (powered) {
                    if (j && l) {
                        m = 6;
                    }
                    if (k && j) {
                        m = 7;
                    }
                    if (l && i) {
                        m = 9;
                    }
                    if (i && k) {
                        m = 8;
                    }
                } else {
                    if (i && k) {
                        m = 8;
                    }
                    if (l && i) {
                        m = 9;
                    }
                    if (k && j) {
                        m = 7;
                    }
                    if (j && l) {
                        m = 6;
                    }
                }
            }
            if (m == 0) {
                if (this.world.getBlock(this.x, this.y + 1, this.z - 1) == RailBlock.this.id) {
                    m = 4;
                }
                if (this.world.getBlock(this.x, this.y + 1, this.z + 1) == RailBlock.this.id) {
                    m = 5;
                }
            }
            if (m == 1) {
                if (this.world.getBlock(this.x + 1, this.y + 1, this.z) == RailBlock.this.id) {
                    m = 2;
                }
                if (this.world.getBlock(this.x - 1, this.y + 1, this.z) == RailBlock.this.id) {
                    m = 3;
                }
            }
            if (m < 0) {
                m = 0;
            }
            this.metadata = m;
            this.updateConnections();
            this.world.setBlockMetadata(this.x, this.y, this.z, m);
            for (int n = 0; n < this.connections.size(); ++n) {
                RailNode railNode = this.getNeighborRail((BlockPos)this.connections.get(n));
                if (railNode == null) continue;
                railNode.removeSoftConnections();
                if (!railNode.canConnectTo(this)) continue;
                railNode.addConnection(this);
            }
        }
    }
}

