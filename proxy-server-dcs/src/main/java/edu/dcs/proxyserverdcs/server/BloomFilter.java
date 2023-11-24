package edu.dcs.proxyserverdcs.server;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.hash.Hashing;

@Component
public class BloomFilter {
    
    private BitSet filter;
    private int size;
    private int numHashFunctions;

    @Autowired
    public BloomFilter(@Value("${filter.size}") int size, @Value("${filter.numhashfun}") int numHashFunctions) {
        this.filter = new BitSet(size);
        this.size = size;
        this.numHashFunctions = numHashFunctions;
        this.filter.set(5);
        this.filter.set(12);
        this.filter.set(30);
    }

    public BloomFilter() {}

    public void add(String element) {

        if (numHashFunctions == 2) {
            filter.set(hashFunction1(element) % size, true);
            filter.set(hashFunction2(element) % size, true);
        } else if (numHashFunctions == 3) {
            filter.set(hashFunction1(element) % size, true);
            filter.set(hashFunction2(element) % size, true);


            filter.set(hashFunction3(element) % size, true);
        }
    }

    public boolean contains(String element) {

        if (numHashFunctions == 2) {
            if (!filter.get(hashFunction1(element) % size)) {
                return false;
            }
            if (!filter.get(hashFunction2(element) % size)) {
                return false;
            }
        } else if (numHashFunctions == 3) {
            if (!filter.get(hashFunction1(element) % size)) {
                return false;
            }
            if (!filter.get(hashFunction2(element) % size)) {
                return false;
            }
            if (!filter.get(hashFunction3(element) % size)) {
                return false;
            }
        }
        return true;
    }

    private int hashFunction1(String element) {
        return element.hashCode();
    }

    private int hashFunction2(String element) {
        int hash = 5381;
        for (int i = 0; i < element.length(); i++) {
            hash = ((hash << 5) + hash) + element.charAt(i);
        }
        return hash;
    }

    private int hashFunction3(String element) {
        return Hashing.murmur3_128().hashString(element, StandardCharsets.UTF_8).asInt();
    }

    public BitSet getBitSet() {
        return filter;
    }

    public int getsize() {
        return size;
    }

}

