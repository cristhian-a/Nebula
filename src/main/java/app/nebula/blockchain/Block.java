package app.nebula.blockchain;

import java.util.Objects;

public record Block(int index, long timestamp, String data, String previousHash, long nonce, String hash) {

    public Block {
        data = Objects.requireNonNull(data);
        previousHash = Objects.requireNonNull(previousHash);

        hash = hash == null
                ? Hashing.calculateHash(index, timestamp, data, previousHash, nonce)
                : hash;
    }

    public static String calculateHash(int index, long timestamp, String data, String previousHash, long nonce) {
        return Hashing.calculateHash(index, timestamp, data, previousHash, nonce);
    }

    public String calculateHash() {
        return calculateHash(index, timestamp, data, previousHash, nonce);
    }
}
