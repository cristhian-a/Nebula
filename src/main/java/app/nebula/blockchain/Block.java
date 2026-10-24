package app.nebula.blockchain;

import java.util.Objects;

public record Block(int index, long timestamp, String data, String previousHash, String hash) {

    public Block(int index, long timestamp, String data, String previousHash) {
        this(index, timestamp, data, previousHash, null);
    }

    public Block {
        data = Objects.requireNonNull(data);
        previousHash = Objects.requireNonNull(previousHash);

        hash = hash == null
                ? Hashing.calculateHash(index, timestamp, data, previousHash)
                : hash;
    }

    public String calculateHash() {
        return Hashing.calculateHash(index, timestamp, data, previousHash);
    }
}
