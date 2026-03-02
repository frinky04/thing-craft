/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.ai.pathing;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.BinaryHeap;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.util.Int2ObjectHashMap;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;

public class PathFinder {
    private WorldView world;
    private BinaryHeap heap = new BinaryHeap();
    private Int2ObjectHashMap nodes = new Int2ObjectHashMap();
    private PathNode[] neighbors = new PathNode[32];

    public PathFinder(WorldView world) {
        this.world = world;
    }

    public Path findPath(Entity entity, Entity target, float range) {
        return this.findPath(entity, target.x, target.shape.minY, target.z, range);
    }

    public Path findPath(Entity entity, int x, int y, int z, float range) {
        return this.findPath(entity, (float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, range);
    }

    private Path findPath(Entity entity, double x, double y, double z, float range) {
        this.heap.clear();
        this.nodes.clear();
        PathNode pathNode = this.getOrAddNode(MathHelper.floor(entity.shape.minX), MathHelper.floor(entity.shape.minY), MathHelper.floor(entity.shape.minZ));
        PathNode pathNode2 = this.getOrAddNode(MathHelper.floor(x - (double)(entity.width / 2.0f)), MathHelper.floor(y), MathHelper.floor(z - (double)(entity.width / 2.0f)));
        PathNode pathNode3 = new PathNode(MathHelper.floor(entity.width + 1.0f), MathHelper.floor(entity.height + 1.0f), MathHelper.floor(entity.width + 1.0f));
        Path path = this.buildPath(entity, pathNode, pathNode2, pathNode3, range);
        return path;
    }

    private Path buildPath(Entity entity, PathNode start, PathNode target, PathNode bounds, float range) {
        start.distanceFromStart = 0.0f;
        start.weight = start.distanceToTarget = start.distanceTo(target);
        this.heap.clear();
        this.heap.insert(start);
        PathNode pathNode = start;
        while (!this.heap.isEmpty()) {
            PathNode pathNode2 = this.heap.pop();
            if (pathNode2.hash == target.hash) {
                return this.buildPath(start, target);
            }
            if (pathNode2.distanceTo(target) < pathNode.distanceTo(target)) {
                pathNode = pathNode2;
            }
            pathNode2.visited = true;
            int i = this.getNeighbors(entity, pathNode2, bounds, target, range);
            for (int j = 0; j < i; ++j) {
                PathNode pathNode3 = this.neighbors[j];
                float f = pathNode2.distanceFromStart + pathNode2.distanceTo(pathNode3);
                if (pathNode3.isInHeap() && !(f < pathNode3.distanceFromStart)) continue;
                pathNode3.prev = pathNode2;
                pathNode3.distanceFromStart = f;
                pathNode3.distanceToTarget = pathNode3.distanceTo(target);
                if (pathNode3.isInHeap()) {
                    this.heap.setWeight(pathNode3, pathNode3.distanceFromStart + pathNode3.distanceToTarget);
                    continue;
                }
                pathNode3.weight = pathNode3.distanceFromStart + pathNode3.distanceToTarget;
                this.heap.insert(pathNode3);
            }
        }
        if (pathNode == start) {
            return null;
        }
        return this.buildPath(start, pathNode);
    }

    private int getNeighbors(Entity entity, PathNode node, PathNode bounds, PathNode target, float range) {
        int i = 0;
        int j = 0;
        if (this.getBlockingType(entity, node.x, node.y + 1, node.z, bounds) > 0) {
            j = 1;
        }
        PathNode pathNode = this.getValidNode(entity, node.x, node.y, node.z + 1, bounds, j);
        PathNode pathNode2 = this.getValidNode(entity, node.x - 1, node.y, node.z, bounds, j);
        PathNode pathNode3 = this.getValidNode(entity, node.x + 1, node.y, node.z, bounds, j);
        PathNode pathNode4 = this.getValidNode(entity, node.x, node.y, node.z - 1, bounds, j);
        if (pathNode != null && !pathNode.visited && pathNode.distanceTo(target) < range) {
            this.neighbors[i++] = pathNode;
        }
        if (pathNode2 != null && !pathNode2.visited && pathNode2.distanceTo(target) < range) {
            this.neighbors[i++] = pathNode2;
        }
        if (pathNode3 != null && !pathNode3.visited && pathNode3.distanceTo(target) < range) {
            this.neighbors[i++] = pathNode3;
        }
        if (pathNode4 != null && !pathNode4.visited && pathNode4.distanceTo(target) < range) {
            this.neighbors[i++] = pathNode4;
        }
        return i;
    }

    private PathNode getValidNode(Entity entity, int x, int y, int z, PathNode bounds, int neighborBlockingType) {
        PathNode pathNode;
        Object object = null;
        if (this.getBlockingType(entity, x, y, z, bounds) > 0) {
            pathNode = this.getOrAddNode(x, y, z);
        }
        if (pathNode == null && this.getBlockingType(entity, x, y + neighborBlockingType, z, bounds) > 0) {
            pathNode = this.getOrAddNode(x, y + neighborBlockingType, z);
            y += neighborBlockingType;
        }
        if (pathNode != null) {
            int i = 0;
            int j = 0;
            while (y > 0 && (j = this.getBlockingType(entity, x, y - 1, z, bounds)) > 0) {
                if (j < 0) {
                    return null;
                }
                if (++i >= 4) {
                    return null;
                }
                --y;
            }
            if (y > 0) {
                pathNode = this.getOrAddNode(x, y, z);
            }
        }
        return pathNode;
    }

    private final PathNode getOrAddNode(int x, int y, int z) {
        int i = x | y << 10 | z << 20;
        PathNode pathNode = (PathNode)this.nodes.get(i);
        if (pathNode == null) {
            pathNode = new PathNode(x, y, z);
            this.nodes.put(i, pathNode);
        }
        return pathNode;
    }

    private int getBlockingType(Entity entity, int x, int y, int z, PathNode bounds) {
        for (int i = x; i < x + bounds.x; ++i) {
            for (int j = y; j < y + bounds.y; ++j) {
                for (int k = z; k < z + bounds.z; ++k) {
                    Material material = this.world.getMaterial(x, y, z);
                    if (material.blocksMovement()) {
                        return 0;
                    }
                    if (material != Material.WATER && material != Material.LAVA) continue;
                    return -1;
                }
            }
        }
        return 1;
    }

    private Path buildPath(PathNode start, PathNode target) {
        int i = 1;
        PathNode pathNode = target;
        while (pathNode.prev != null) {
            ++i;
            pathNode = pathNode.prev;
        }
        PathNode[] pathNodes = new PathNode[i];
        pathNode = target;
        pathNodes[--i] = pathNode;
        while (pathNode.prev != null) {
            pathNode = pathNode.prev;
            pathNodes[--i] = pathNode;
        }
        return new Path(pathNodes);
    }
}

