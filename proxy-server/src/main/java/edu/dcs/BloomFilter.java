package edu.advanced.dcs;

import java.util.BitSet;

public class BloomFilter {
    private BitSet filter;
    private int size;
    private int numHashFunctions;

    public BloomFilter(int size, int numHashFunctions) {
        this.filter = new BitSet(size);
        this.size = size;
        this.numHashFunctions = numHashFunctions;
    }

    public void add(String element) {
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = hash(element, i);
            filter.set(hash % size, true);
        }
    }

    public boolean contains(String element) {
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = hash(element, i);
            if (!filter.get(hash % size)) {
                return false;
            }
        }
        return true;
    }

    private int hash(String element, int hashFunctionIndex) {
        // You can use different hash functions here
        // For simplicity, we're using the built-in hashCode() method
        return (element.hashCode() + hashFunctionIndex) & Integer.MAX_VALUE;
    }

    public static void main(String[] args) {
        BloomFilter bloomFilter = new BloomFilter(1000, 3);

        bloomFilter.add("apple");
        bloomFilter.add("banana");
        bloomFilter.add("orange");

        System.out.println(bloomFilter.contains("apple"));    // true
        System.out.println(bloomFilter.contains("grape"));    // false (not added)
        System.out.println(bloomFilter.contains("banana"));   // true
        System.out.println(bloomFilter.contains("kiwi"));     // false (not added)
    }
}

