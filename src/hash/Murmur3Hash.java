package com.kv.hash;

// Implementación simplificada de MurmurHash3 de 32 bits
public class Murmur3Hash {
    public static int hash32(byte[] data) {
        int len = data.length;
        int c1 = 0xcc9e2d51;
        int c2 = 0x1b873593;
        int h1 = len;
        int i = 0;
        while (i + 4 <= len) {
            int k1 = (data[i] & 0xff) |
                    ((data[i+1] & 0xff) << 8) |
                    ((data[i+2] & 0xff) << 16) |
                    ((data[i+3] & 0xff) << 24);
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;
            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
            i += 4;
        }
        // tail
        int k1 = 0;
        switch (len - i) {
            case 3: k1 ^= (data[i+2] & 0xff) << 16;
            case 2: k1 ^= (data[i+1] & 0xff) << 8;
            case 1: k1 ^= (data[i] & 0xff);
                k1 *= c1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= c2;
                h1 ^= k1;
        }
        h1 ^= len;
        h1 ^= (h1 >>> 16);
        h1 *= 0x85ebca6b;
        h1 ^= (h1 >>> 13);
        h1 *= 0xc2b2ae35;
        h1 ^= (h1 >>> 16);
        return h1;
    }
}