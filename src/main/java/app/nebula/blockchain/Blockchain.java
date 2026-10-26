package app.nebula.blockchain;

import app.nebula.rule.MonetaryPolicy;

import java.util.*;

public final class Blockchain {

    private static final String GENESIS_PREVIOUS_HASH = "0";

    private final List<Block> blocks = new ArrayList<>();
    private final Map<String, Long> initialBalances;
    private final int difficulty;

    public Blockchain(int difficulty) {
        this(difficulty, new HashMap<>());
    }

    public Blockchain(int difficulty, Map<String, Long> initialBalances) {
        Objects.requireNonNull(initialBalances, "Initial balances cannot be null.");

        if (difficulty < 0 || difficulty > 64)
            throw new IllegalArgumentException("Invalid difficulty");

        this.difficulty = difficulty;
        Wallet adam = new Wallet();
        Wallet eve = new Wallet();
        var transaction = new Transaction(adam.getAddress(), eve.getAddress(), MonetaryPolicy.INITIAL_SUPPLY);
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

        var balances = new HashMap<>(initialBalances);
        balances.put(adam.getAddress(), MonetaryPolicy.INITIAL_SUPPLY);
        this.initialBalances = balances;
    }

    public Block addBlock(List<Transaction> transactions) {
        Objects.requireNonNull(transactions);

        if (anyInvalid(transactions))
            throw new IllegalArgumentException("There is an invalid transaction in the block.");

        LedgerState candidateState = buildLedgerState();
        for (var transaction : transactions) {
            candidateState.apply(transaction);
        }

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

    public LedgerState buildLedgerState() {
        LedgerState ledger = new LedgerState(initialBalances);
        blocks.forEach(b -> b.transactions().forEach(ledger::apply));
        return ledger;
    }

    public boolean isValid() {
        if(!isValid(blocks, difficulty))
            return false;

        try {
            buildLedgerState();
            return true;
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return false;
        }
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

            if (anyInvalid(current.transactions()))
                return false;

            if (i > 0) {
                Block previous = candidateBlocks.get(i - 1);

                if (!current.previousHash().equals(previous.hash()))
                    return false;
            }
        }

        return true;
    }

    private static boolean anyInvalid(List<Transaction> transactions) {
        return !transactions.stream().allMatch(Transaction::verifySignature);
    }
}
