package com.paz.pdsa.dsa.ras;


public class KeyPair {
    private long privateKey;
    private long publicKey;
    private long keyLength;

    public KeyPair() {
    }

    public KeyPair(long privateKey, long publicKey, long keyLength) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.keyLength = keyLength;
    }

    public long getPrivateKey() {
        return privateKey;
    }

    public KeyPair setPrivateKey(long privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public long getPublicKey() {
        return publicKey;
    }

    public KeyPair setPublicKey(long publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public long getKeyLength() {
        return keyLength;
    }

    public KeyPair setKeyLength(long keyLength) {
        this.keyLength = keyLength;
        return this;
    }
}
