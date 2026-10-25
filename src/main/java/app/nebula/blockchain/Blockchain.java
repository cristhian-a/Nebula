package app.nebula.blockchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Blockchain {

    private static final String GENESIS_PREVIOUS_HASH = "0";

    private final List<Block> blocks = new ArrayList<>();
    private final int difficulty;

    public Blockchain(int difficulty) {
        if (difficulty < 0 || difficulty > 64)
            throw new IllegalArgumentException("Invalid difficulty");

        this.difficulty = difficulty;
        Wallet adam = new Wallet();
        Wallet eve = new Wallet();
        var transaction = new Transaction(adam.getAddress(), eve.getAddress(), 1L);
        var signedTransaction = adam.sign(transaction);

        Block genesis = new Block(
                0,
                System.currentTimeMillis(),
                List.of(signedTransaction),
                GENESIS_PREVIOUS_HASH,
                0,
                null
        );

        blocks.add(ProofOfWork.mine(genesis, difficulty));
    }

    public Block addBlock(List<Transaction> transactions) {
        Objects.requireNonNull(transactions);

        if (!areTransactionsValid(transactions))
            throw new IllegalArgumentException("There is an invalid transaction in the block.");

        Block previous = blocks.getLast();

        Block candidate = new Block(
                previous.index() + 1,
                System.currentTimeMillis(),
                transactions,
                previous.hash(),
                0,
                null
        );

        Block mined = ProofOfWork.mine(candidate, difficulty);
        blocks.add(mined);

        return mined;
    }

    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public boolean isValid() {
        return isValid(blocks, difficulty);
    }

    static boolean isValid(List<Block> candidateBlocks, int difficulty) {
        if (candidateBlocks == null || candidateBlocks.isEmpty())
            return false;

        Block genesis = candidateBlocks.getFirst();

        if (genesis.index() != 0 || !GENESIS_PREVIOUS_HASH.equals(genesis.previousHash()))
            return false;

        for (int i = 0; i < candidateBlocks.size(); i++) {
            Block current = candidateBlocks.get(i);

            if (!current.hash().equals(current.calculateHash()))
                return false;

            if (current.index() != i)
                return false;

            if (!ProofOfWork.isValid(current, difficulty))
                return false;

            if (!areTransactionsValid(current.transactions()))
                return false;

            if (i > 0) {
                Block previous = candidateBlocks.get(i - 1);

                if (!current.previousHash().equals(previous.hash()))
                    return false;
            }
        }

        return true;
    }

    private static boolean areTransactionsValid(List<Transaction> transactions) {
        return transactions.stream().allMatch(Transaction::verifySignature);
    }
}
