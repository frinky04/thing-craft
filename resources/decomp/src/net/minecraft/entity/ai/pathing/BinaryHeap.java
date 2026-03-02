/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.ai.pathing.PathNode;

public class BinaryHeap {
    private PathNode[] heap = new PathNode[1024];
    private int size = 0;

    public PathNode insert(PathNode node) {
        if (node.heapIndex >= 0) {
            throw new IllegalStateException("OW KNOWS!");
        }
        if (this.size == this.heap.length) {
            PathNode[] pathNodes = new PathNode[this.size << 1];
            System.arraycopy(this.heap, 0, pathNodes, 0, this.size);
            this.heap = pathNodes;
        }
        this.heap[this.size] = node;
        node.heapIndex = this.size;
        this.upHeap(this.size++);
        return node;
    }

    public void clear() {
        this.size = 0;
    }

    public PathNode pop() {
        PathNode pathNode = this.heap[0];
        this.heap[0] = this.heap[--this.size];
        this.heap[this.size] = null;
        if (this.size > 0) {
            this.shiftDown(0);
        }
        pathNode.heapIndex = -1;
        return pathNode;
    }

    public void setWeight(PathNode node, float weight) {
        float f = node.weight;
        node.weight = weight;
        if (weight < f) {
            this.upHeap(node.heapIndex);
        } else {
            this.shiftDown(node.heapIndex);
        }
    }

    private void upHeap(int index) {
        PathNode pathNode = this.heap[index];
        float f = pathNode.weight;
        while (index > 0) {
            int i = index - 1 >> 1;
            PathNode pathNode2 = this.heap[i];
            if (!(f < pathNode2.weight)) break;
            this.heap[index] = pathNode2;
            pathNode2.heapIndex = index;
            index = i;
        }
        this.heap[index] = pathNode;
        pathNode.heapIndex = index;
    }

    private void shiftDown(int index) {
        PathNode pathNode = this.heap[index];
        float f = pathNode.weight;
        while (true) {
            float k;
            PathNode pathNode3;
            int i = 1 + (index << 1);
            int j = i + 1;
            if (i >= this.size) break;
            PathNode pathNode2 = this.heap[i];
            float g = pathNode2.weight;
            if (j >= this.size) {
                Object object = null;
                float f2 = Float.POSITIVE_INFINITY;
            } else {
                pathNode3 = this.heap[j];
                k = pathNode3.weight;
            }
            if (g < k) {
                if (!(g < f)) break;
                this.heap[index] = pathNode2;
                pathNode2.heapIndex = index;
                index = i;
                continue;
            }
            if (!(k < f)) break;
            this.heap[index] = pathNode3;
            pathNode3.heapIndex = index;
            index = j;
        }
        this.heap[index] = pathNode;
        pathNode.heapIndex = index;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }
}

