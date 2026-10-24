package app.nebula.blockchain;

import java.util.List;
import java.util.Objects;

public record Block(int index, long timestamp, List<Transaction> transactions, String previousHash, long nonce, String hash) {

    public Block {
        transactions = List.copyOf(Objects.requireNonNull(transactions));
        previousHash = Objects.requireNonNull(previousHash);

        hash = hash == null
                ? Hashing.calculateHash(index, timestamp, transactions, previousHash, nonce)
                : hash;
    }

    public static String calculateHash(
            int index, long timestamp, List<Transaction> transactions, String previousHash, long nonce
    ) {
        return Hashing.calculateHash(index, timestamp, transactions, previousHash, nonce);
    }

    public String calculateHash() {
        return calculateHash(index, timestamp, transactions, previousHash, nonce);
    }
}
