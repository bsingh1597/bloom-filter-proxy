package edu.dcs.proxyserverdcs.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
        // this.filter.set(5);
        // this.filter.set(12);
        // this.filter.set(30);
    }

    public BloomFilter() {}

    public void add(String element) {

        for (int i = 0; i < numHashFunctions; i++) {
            int hash = hash(element, i);
            filter.set(hash);
        }
    }

    public boolean contains(String element) {

        for (int i = 0; i < numHashFunctions; i++) {
            int hash = hash(element, i);
            if (!this.filter.get(hash)) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(BitSet bitsetArray, String element) {

        for (int i = 0; i < numHashFunctions; i++) {
            int hash = hash(element, i);
            if (!bitsetArray.get(hash)) {
                return false;
            }
        }
        return true;
    }

    private int hash(String element, int seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest((seed + element).getBytes(StandardCharsets.UTF_8));
            return Math.abs(java.util.Arrays.hashCode(hashBytes) % size);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash function not available", e);
        }
    }

    /*
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

    public boolean contains(BitSet filter, String element) {

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

     */

    /* 
    public int hashFunction1(String element) {
        return element.hashCode();
    }

    public int hashFunction2(String element) {
        int hash = 5381;
        for (int i = 0; i < element.length(); i++) {
            hash = ((hash << 5) + hash) + element.charAt(i);
        }
        return hash;
    }

    public int hashFunction3(String element) {
        return Hashing.murmur3_32_fixed().hashString(element, StandardCharsets.UTF_8).asInt();
    }
    */
    

    public BitSet getBitSet() { 
        return filter;
    }

    public int getsize() {
        return size;
    }

}

