/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class Int2ObjectHashMap {
    private transient Node[] nodes = new Node[16];
    private transient int size;
    private int threshold = 12;
    private final float loadFactor;
    private volatile transient int modCount;

    public Int2ObjectHashMap() {
        this.loadFactor = 0.75f;
    }

    private static int hash(int key) {
        key ^= key >>> 20 ^ key >>> 12;
        return key ^ key >>> 7 ^ key >>> 4;
    }

    private static int index(int hash, int cap) {
        return hash & cap - 1;
    }

    public Object get(int key) {
        int i = Int2ObjectHashMap.hash(key);
        Node node = this.nodes[Int2ObjectHashMap.index(i, this.nodes.length)];
        while (node != null) {
            if (node.key == key) {
                return node.value;
            }
            node = node.next;
        }
        return null;
    }

    @Environment(value=EnvType.SERVER)
    public boolean containsKey(int key) {
        return this.getNode(key) != null;
    }

    @Environment(value=EnvType.SERVER)
    final Node getNode(int key) {
        int i = Int2ObjectHashMap.hash(key);
        Node node = this.nodes[Int2ObjectHashMap.index(i, this.nodes.length)];
        while (node != null) {
            if (node.key == key) {
                return node;
            }
            node = node.next;
        }
        return null;
    }

    public void put(int key, Object value) {
        int i = Int2ObjectHashMap.hash(key);
        int j = Int2ObjectHashMap.index(i, this.nodes.length);
        Node node = this.nodes[j];
        while (node != null) {
            if (node.key == key) {
                node.value = value;
            }
            node = node.next;
        }
        ++this.modCount;
        this.insertNode(i, key, value, j);
    }

    private void resize(int size) {
        Node[] nodes = this.nodes;
        int i = nodes.length;
        if (i == 0x40000000) {
            this.threshold = Integer.MAX_VALUE;
            return;
        }
        Node[] nodes2 = new Node[size];
        this.addAll(nodes2);
        this.nodes = nodes2;
        this.threshold = (int)((float)size * this.loadFactor);
    }

    private void addAll(Node[] nodes) {
        Node[] nodes2 = this.nodes;
        int i = nodes.length;
        for (int j = 0; j < nodes2.length; ++j) {
            Node node2;
            Node node = nodes2[j];
            if (node == null) continue;
            nodes2[j] = null;
            do {
                node2 = node.next;
                int k = Int2ObjectHashMap.index(node.hash, i);
                node.next = nodes[k];
                nodes[k] = node;
            } while ((node = node2) != null);
        }
    }

    public Object remove(int key) {
        Node node = this.removeNode(key);
        return node == null ? null : node.value;
    }

    final Node removeNode(int key) {
        Node node;
        int i = Int2ObjectHashMap.hash(key);
        int j = Int2ObjectHashMap.index(i, this.nodes.length);
        Node node2 = node = this.nodes[j];
        while (node2 != null) {
            Node node3 = node2.next;
            if (node2.key == key) {
                ++this.modCount;
                --this.size;
                if (node == node2) {
                    this.nodes[j] = node3;
                } else {
                    node.next = node3;
                }
                return node2;
            }
            node = node2;
            node2 = node3;
        }
        return node2;
    }

    public void clear() {
        ++this.modCount;
        Node[] nodes = this.nodes;
        for (int i = 0; i < nodes.length; ++i) {
            nodes[i] = null;
        }
        this.size = 0;
    }

    private void insertNode(int hash, int key, Object value, int index) {
        Node node = this.nodes[index];
        this.nodes[index] = new Node(hash, key, value, node);
        if (this.size++ >= this.threshold) {
            this.resize(2 * this.nodes.length);
        }
    }

    static class Node {
        final int key;
        Object value;
        Node next;
        final int hash;

        Node(int hash, int key, Object value, Node next) {
            this.value = value;
            this.next = next;
            this.key = key;
            this.hash = hash;
        }

        public final int getKey() {
            return this.key;
        }

        public final Object getValue() {
            return this.value;
        }

        public final boolean equals(Object object) {
            Object object3;
            Object object2;
            Integer integer2;
            if (!(object instanceof Node)) {
                return false;
            }
            Node node = (Node)object;
            Integer integer = this.getKey();
            return (integer == (integer2 = Integer.valueOf(node.getKey())) || integer != null && ((Object)integer).equals(integer2)) && ((object2 = this.getValue()) == (object3 = node.getValue()) || object2 != null && object2.equals(object3));
        }

        public final int hashCode() {
            return Int2ObjectHashMap.hash(this.key);
        }

        public final String toString() {
            return this.getKey() + "=" + this.getValue();
        }
    }
}

