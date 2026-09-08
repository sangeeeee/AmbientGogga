package com.sange.ambientgogga.client;

import java.util.Arrays;

/** Bounded primitive-key cache. Collisions only cause a fresh query, never reuse another cell's value. */
public final class SeaFireTickCache<T> {
    private final long[] keys, ticks;
    private final Object[] values;

    public SeaFireTickCache(int capacity) {
        if (Integer.bitCount(capacity) != 1 || capacity <= 0) throw new IllegalArgumentException("Capacity must be a positive power of two");
        keys = new long[capacity];
        ticks = new long[capacity];
        values = new Object[capacity];
    }

    private int slot(long key) {
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        return (int) key & (values.length - 1);
    }

    @SuppressWarnings("unchecked")
    public T get(long key, long tick) {
        int slot = slot(key);
        return keys[slot] == key && ticks[slot] == tick ? (T) values[slot] : null;
    }

    public void put(long key, long tick, T value) {
        int slot = slot(key);
        keys[slot] = key;
        ticks[slot] = tick;
        values[slot] = value;
    }

    public void clear() { Arrays.fill(values, null); }
}
