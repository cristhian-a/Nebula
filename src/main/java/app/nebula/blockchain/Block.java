package app.nebula.blockchain;

import java.util.List;
import java.util.Objects;

public record Block(
        int index,
        long timestamp,
        List<Transaction> transactions,
        String previousHash,
        long nonce,
        String hash,
        MiningReward miningReward
) {

    public Block(
            int index,
            long timestamp,
            List<Transaction> transactions,
            String previousHash,
            long nonce,
            String hash
    ) {
        this(index, timestamp, transactions, previousHash, nonce, hash, null);
    }

    public Block {
        transactions = List.copyOf(Objects.requireNonNull(transactions));
        previousHash = Objects.requireNonNull(previousHash);

        hash = hash == null
                ? calculateHash(index, timestamp, transactions, previousHash, nonce, miningReward)
                : hash;
    }

    public static String calculateHash(int index, long timestamp, List<Transaction> transactions, String previousHash,
                                       long nonce, MiningReward miningReward
    ) {
        return Hashing.calculateHash(index, timestamp, transactions, previousHash, nonce, miningReward);
    }

    public String calculateHash() {
        return calculateHash(index, timestamp, transactions, previousHash, nonce, miningReward);
    }
}
