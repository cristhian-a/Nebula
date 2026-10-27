package app.nebula.blockchain;

import app.nebula.rule.MonetaryPolicy;

import java.util.*;

public final class Blockchain {

    private static final String GENESIS_PREVIOUS_HASH = "0";

    private final List<Block> blocks = new ArrayList<>();
    private final int difficulty;

    public Blockchain(int difficulty) {
        if (difficulty < 0 || difficulty > 64) throw new IllegalArgumentException("Invalid difficulty");

        this.difficulty = difficulty;

        Block genesis = new Block(0, System.currentTimeMillis(), List.of(), GENESIS_PREVIOUS_HASH, 0, null);

        blocks.add(ProofOfWork.mine(genesis, difficulty));
    }

    public Blockchain(List<Block> blocks, int difficulty) {
        Objects.requireNonNull(blocks);
        if (difficulty < 0 || difficulty > 64) throw new IllegalArgumentException("Invalid difficulty");

        this.difficulty = difficulty;
        this.blocks.addAll(blocks);

        if (this.blocks.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Blocks cannot contain null elements.");
        }
    }

    public Block addBlock(List<Transaction> transactions, String minerAddress) {
        Objects.requireNonNull(transactions);
        Objects.requireNonNull(minerAddress);

        if (anyInvalid(transactions))
            throw new IllegalArgumentException("There is an invalid transaction in the block.");

        if (minerAddress.isBlank()) throw new IllegalArgumentException("Miner address cannot be blank.");

        LedgerState candidateState = buildLedger();
        for (var transaction : transactions) {
            candidateState.apply(transaction);
        }

        long rewardAmount = MonetaryPolicy.calculateReward(candidateState.getTotalSupplyCents());
        MiningReward reward = new MiningReward(minerAddress, rewardAmount);

        Block previous = blocks.getLast();

        Block candidate = new Block(previous.index() + 1, System.currentTimeMillis(), transactions, previous.hash(), 0, null, reward);

        Block mined = ProofOfWork.mine(candidate, difficulty);
        blocks.add(mined);

        return mined;
    }

    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public LedgerState buildLedger() {
        LedgerState ledger = new LedgerState();

        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            block.transactions().forEach(ledger::apply);

            MiningReward reward = block.miningReward();
            if (i == 0) {
                if (reward != null) throw new IllegalArgumentException("Genesis block cannot contain a mining reward.");

                continue;
            }

            if (reward == null) throw new IllegalArgumentException("Non-genesis blocks must contain a mining reward.");

            long expectedReward = MonetaryPolicy.calculateReward(ledger.getTotalSupplyCents());

            if (reward.amount() != expectedReward)
                throw new IllegalArgumentException("Invalid mining reward. Expected " + expectedReward + " cents, but received " + reward.amount() + " cents.");

            ledger.applyReward(reward);
        }
        return ledger;
    }

    public boolean isValid() {
        if (blocks.isEmpty()) return false;

        Block genesis = blocks.getFirst();

        if (genesis.index() != 0 || !GENESIS_PREVIOUS_HASH.equals(genesis.previousHash())) return false;
        if (!genesis.transactions().isEmpty()) return false;

        for (int i = 0; i < blocks.size(); i++) {
            Block current = blocks.get(i);

            if (!current.hash().equals(current.calculateHash())) return false;

            if (current.index() != i) return false;

            if (!ProofOfWork.isValid(current, difficulty)) return false;

            if (anyInvalid(current.transactions())) return false;

            if (i > 0) {
                Block previous = blocks.get(i - 1);

                if (!current.previousHash().equals(previous.hash())) return false;
            }
        }

        try {
            buildLedger();
            return true;
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return false;
        }
    }

    private static boolean anyInvalid(List<Transaction> transactions) {
        return !transactions.stream().allMatch(Transaction::verifySignature);
    }
}
