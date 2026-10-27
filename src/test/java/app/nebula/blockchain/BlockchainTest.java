package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class BlockchainTest {

    private static final int DIFFICULTY = 4;

    @Test
    void newBlockchainShouldBeValid() {
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        assertEquals(1, blockchain.getBlocks().size());
        assertTrue(blockchain.isValid());
    }

    @Test
    void newBlockchainShouldStartWithZeroSupplyAndRewardlessGenesis() {
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        assertEquals(1, blockchain.getBlocks().size());

        Block genesis = blockchain.getBlocks().getFirst();
        assertEquals(0, genesis.index());
        assertTrue(genesis.transactions().isEmpty());
        assertNull(genesis.miningReward());

        LedgerState state = blockchain.buildLedger();
        assertEquals(0L, state.getTotalSupplyCents());
        assertTrue(blockchain.isValid());
    }

    @Test
    void addingBlocksShouldPreserveValidity() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet carol = new Wallet();

        Blockchain blockchain = new Blockchain(DIFFICULTY);

        // Alice starts with zero coins and mines her first reward.
        blockchain.addBlock(List.of(), alice.getAddress());

        assertEquals(
                app.nebula.rule.MonetaryPolicy.BOOTSTRAP_REWARD_CENTS,
                blockchain.buildLedger().getBalance(alice.getAddress())
        );

        // Alice funds Bob; Bob can then spend part of what he received.
        blockchain.addBlock(
                List.of(
                        sign(alice, bob, 10, 1_000L),
                        sign(bob, carol, 3, 2_000L)
                ),
                carol.getAddress()
        );

        blockchain.addBlock(
                List.of(sign(alice, carol, 5, 3_000L)),
                carol.getAddress()
        );

        assertEquals(4, blockchain.getBlocks().size());
        assertTrue(blockchain.isValid());
    }

    @Test
    void addingBlockWithUnsignedTransactionShouldBeRejected() {
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet miner = new Wallet();

        Transaction unsigned = unsignedTransaction(
                alice.getAddress(), bob.getAddress(), 10, 1_000L
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> blockchain.addBlock(List.of(unsigned), miner.getAddress())
        );

        assertEquals(1, blockchain.getBlocks().size());
        assertTrue(blockchain.isValid());
    }

    @Test
    void modifyingTransactionWithoutUpdatingHashShouldInvalidateChain() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Blockchain blockchain = new Blockchain(DIFFICULTY);

        blockchain.addBlock(List.of(), alice.getAddress());

        Transaction signed = sign(alice, bob, 10, 1_000L);
        blockchain.addBlock(List.of(signed), alice.getAddress());

        List<Block> tamperedBlocks = new ArrayList<>(blockchain.getBlocks());
        Block original = tamperedBlocks.get(2);

        Transaction tamperedTransaction = new Transaction(
                signed.sender(),
                signed.recipient(),
                999,
                signed.timestamp(),
                signed.senderPublicKey(),
                signed.signature()
        );

        Block tampered = new Block(
                original.index(),
                original.timestamp(),
                List.of(tamperedTransaction),
                original.previousHash(),
                original.nonce(),
                original.hash(),
                original.miningReward()
        );

        tamperedBlocks.set(2, tampered);

        Blockchain tamperedBlockchain = new Blockchain(tamperedBlocks, DIFFICULTY);

        assertFalse(tamperedBlockchain.isValid());
        assertTrue(blockchain.isValid());
    }

    @Test
    void recalculatedAndReminedBlockWithInvalidSignatureShouldBeRejected() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet carol = new Wallet();

        Blockchain blockchain = new Blockchain(DIFFICULTY);

        blockchain.addBlock(List.of(), alice.getAddress());

        Transaction signed = sign(alice, bob, 10, 1_000L);
        blockchain.addBlock(List.of(signed), alice.getAddress());

        List<Block> tamperedBlocks = new ArrayList<>(blockchain.getBlocks());
        Block original = tamperedBlocks.get(2);

        // Change the recipient but preserve the original signature.
        Transaction tamperedTransaction = new Transaction(
                signed.sender(),
                carol.getAddress(),
                signed.amount(),
                signed.timestamp(),
                signed.senderPublicKey(),
                signed.signature()
        );

        // Recalculate the hash and mine the modified block.
        Block candidate = new Block(
                original.index(),
                original.timestamp(),
                List.of(tamperedTransaction),
                original.previousHash(),
                0,
                null,
                original.miningReward()
        );

        Block remined = ProofOfWork.mine(candidate, DIFFICULTY);
        tamperedBlocks.set(2, remined);

        assertTrue(ProofOfWork.isValid(remined, DIFFICULTY));

        Blockchain tamperedBlockchain = new Blockchain(tamperedBlocks, DIFFICULTY);

        assertFalse(tamperedBlockchain.isValid());
        assertTrue(blockchain.isValid());
    }

    @Test
    void changingTransactionShouldChangeBlockHash() {
        Transaction original = unsignedTransaction(
                "Alice", "Bob", 10, 1_000L
        );

        Transaction modified = unsignedTransaction(
                "Alice", "Bob", 11, 1_000L
        );

        Block originalBlock = createBlock(List.of(original));
        Block modifiedBlock = createBlock(List.of(modified));

        assertNotEquals(originalBlock.hash(), modifiedBlock.hash());
    }

    @Test
    void reorderingTransactionsShouldChangeBlockHash() {
        Transaction first = unsignedTransaction(
                "Alice", "Bob", 10, 1_000L
        );

        Transaction second = unsignedTransaction(
                "Bob", "Carol", 5, 2_000L
        );

        Block originalBlock = createBlock(List.of(first, second));
        Block reorderedBlock = createBlock(List.of(second, first));

        assertNotEquals(originalBlock.hash(), reorderedBlock.hash());
    }

    @Test
    void changingSignatureShouldChangeBlockHash() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Transaction signed = sign(alice, bob, 10, 1_000L);

        byte[] modifiedSignature = signed.signature();
        modifiedSignature[0] ^= 1;

        Transaction modified = new Transaction(
                signed.sender(),
                signed.recipient(),
                signed.amount(),
                signed.timestamp(),
                signed.senderPublicKey(),
                modifiedSignature
        );

        Block originalBlock = createBlock(List.of(signed));
        Block modifiedBlock = createBlock(List.of(modified));

        assertNotEquals(originalBlock.hash(), modifiedBlock.hash());
        assertFalse(modified.verifySignature());
    }

    @Test
    void changingMiningRewardShouldChangeBlockHash() {
        MiningReward firstReward = new MiningReward("miner-one", 1_000L);
        MiningReward differentAmount = new MiningReward("miner-one", 1_001L);
        MiningReward differentRecipient = new MiningReward("miner-two", 1_000L);

        Block original = createBlock(List.of(), firstReward);
        Block changedAmount = createBlock(List.of(), differentAmount);
        Block changedRecipient = createBlock(List.of(), differentRecipient);

        assertNotEquals(original.hash(), changedAmount.hash());
        assertNotEquals(original.hash(), changedRecipient.hash());
        assertEquals(original.hash(), original.calculateHash());
    }

    @Test
    void blockShouldNotBeAffectedByChangesToOriginalTransactionList() {
        List<Transaction> transactions = new ArrayList<>();

        transactions.add(unsignedTransaction(
                "Alice", "Bob", 10, 1_000L
        ));

        Block block = createBlock(transactions);
        String originalHash = block.hash();

        transactions.add(unsignedTransaction(
                "Bob", "Carol", 5, 2_000L
        ));

        assertEquals(1, block.transactions().size());
        assertEquals(originalHash, block.hash());
        assertEquals(block.hash(), block.calculateHash());
    }

    @Test
    void blockTransactionListShouldBeImmutable() {
        Block block = createBlock(List.of(
                unsignedTransaction("Alice", "Bob", 10, 1_000L)
        ));

        assertThrows(
                UnsupportedOperationException.class,
                () -> block.transactions().add(
                        unsignedTransaction("Bob", "Carol", 5, 2_000L)
                )
        );
    }

    @Test
    void blockWithRecalculatedHashButInsufficientProofOfWorkShouldBeInvalid() {
        Block original = createBlock(List.of(
                unsignedTransaction("Alice", "Bob", 10, 1_000L)
        ));

        Block recalculated = new Block(
                original.index(),
                original.timestamp(),
                List.of(unsignedTransaction("Alice", "Bob", 999, 1_000L)),
                original.previousHash(),
                original.nonce(),
                null,
                original.miningReward()
        );

        assertFalse(ProofOfWork.isValid(recalculated, 64));
    }

    @Test
    void reconstructsBalancesByReplayingTransactionsAndRewardsInBlockOrder() {
        Wallet adam = new Wallet();
        Wallet eve = new Wallet();
        Wallet miner = new Wallet();

        Blockchain blockchain = new Blockchain(DIFFICULTY);

        // Adam has no coins initially. His first block creates the first coins.
        long initialSupply = blockchain.buildLedger().getTotalSupplyCents();
        assertEquals(0L, initialSupply);

        Block firstBlock = blockchain.addBlock(List.of(), adam.getAddress());
        long firstReward = firstBlock.miningReward().amount();

        assertEquals(
                app.nebula.rule.MonetaryPolicy.BOOTSTRAP_REWARD_CENTS,
                firstReward
        );

        // Adam transfers 30 cents to Eve.
        Transaction first = adam.sign(
                new Transaction(adam.getAddress(), eve.getAddress(), 30L)
        );
        Block secondBlock = blockchain.addBlock(List.of(first), miner.getAddress());

        // Eve sends 10 cents back to Adam.
        Transaction second = eve.sign(
                new Transaction(eve.getAddress(), adam.getAddress(), 10L)
        );
        Block thirdBlock = blockchain.addBlock(List.of(second), miner.getAddress());

        LedgerState state = blockchain.buildLedger();

        long secondReward = secondBlock.miningReward().amount();
        long thirdReward = thirdBlock.miningReward().amount();

        assertEquals(firstReward - 30L + 10L,
                state.getBalance(adam.getAddress()));
        assertEquals(20L, state.getBalance(eve.getAddress()));
        assertEquals(secondReward + thirdReward,
                state.getBalance(miner.getAddress()));

        assertEquals(
                firstReward + secondReward + thirdReward,
                state.getTotalSupplyCents()
        );

        assertTrue(blockchain.isValid());
    }

    @Test
    void newBlockShouldPayThePolicyRewardToTheMiner() {
        Blockchain blockchain = new Blockchain(DIFFICULTY);
        Wallet miner = new Wallet();

        long initialSupply = blockchain.buildLedger().getTotalSupplyCents();
        long expectedReward = app.nebula.rule.MonetaryPolicy.calculateReward(initialSupply);

        Block block = blockchain.addBlock(List.of(), miner.getAddress());

        assertNotNull(block.miningReward());
        assertEquals(miner.getAddress(), block.miningReward().recipient());
        assertEquals(expectedReward, block.miningReward().amount());

        LedgerState state = blockchain.buildLedger();
        assertEquals(expectedReward, state.getBalance(miner.getAddress()));
        assertEquals(initialSupply + expectedReward, state.getTotalSupplyCents());
        assertTrue(blockchain.isValid());
    }

    @Test
    void rejectsBlockWithIncorrectMiningRewardEvenWhenProofOfWorkIsValid() {
        Wallet miner = new Wallet();
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        // Mine the first block to establish a legitimate reward.
        blockchain.addBlock(List.of(), miner.getAddress());

        assertTrue(blockchain.isValid());

        Block originalBlock = blockchain.getBlocks().getLast();

        // Create a block with an incorrect reward.
        MiningReward incorrectReward = new MiningReward(
                miner.getAddress(),
                originalBlock.miningReward().amount() + 1L
        );

        Block tamperedBlock = new Block(
                originalBlock.index(),
                originalBlock.timestamp(),
                originalBlock.transactions(),
                originalBlock.previousHash(),
                0L,
                null,
                incorrectReward
        );

        // Re-mine it so that the block has valid proof of work.
        Block minedTamperedBlock = ProofOfWork.mine(
                tamperedBlock,
                DIFFICULTY
        );

        assertTrue(
                ProofOfWork.isValid(minedTamperedBlock, DIFFICULTY),
                "The tampered block should have valid proof of work"
        );

        // Replace the original block with the tampered one.
        List<Block> tamperedBlocks = new java.util.ArrayList<>(
                blockchain.getBlocks()
        );

        tamperedBlocks.set(
                tamperedBlocks.size() - 1,
                minedTamperedBlock
        );

        Blockchain tamperedBlockchain = new Blockchain(tamperedBlocks, DIFFICULTY);

        assertFalse(
                tamperedBlockchain.isValid(),
                "A block with an incorrect reward must be rejected"
        );
    }

    private static Transaction sign(
            Wallet sender,
            Wallet recipient,
            long amount,
            long timestamp
    ) {
        return sender.sign(unsignedTransaction(
                sender.getAddress(),
                recipient.getAddress(),
                amount,
                timestamp
        ));
    }

    private static Transaction unsignedTransaction(
            String sender,
            String recipient,
            long amount,
            long timestamp
    ) {
        return new Transaction(
                sender,
                recipient,
                amount,
                timestamp,
                null,
                null
        );
    }

    private static Block createBlock(List<Transaction> transactions) {
        return createBlock(transactions, null);
    }

    private static Block createBlock(
            List<Transaction> transactions,
            MiningReward miningReward
    ) {
        return new Block(
                1,
                10_000L,
                transactions,
                "previous-hash",
                0,
                null,
                miningReward
        );
    }
}
