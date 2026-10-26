
package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    void addingBlocksShouldPreserveValidity() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet carol = new Wallet();

        var balances = Map.of(alice.getAddress(), 15L);
        Blockchain blockchain = new Blockchain(DIFFICULTY, balances);

        blockchain.addBlock(List.of(
                sign(alice, bob, 10, 1_000L)
        ));

        blockchain.addBlock(List.of(
                sign(bob, carol, 3, 2_000L),
                sign(alice, carol, 5, 3_000L)
        ));

        assertEquals(3, blockchain.getBlocks().size());
        assertTrue(blockchain.isValid());
    }

    @Test
    void addingBlockWithUnsignedTransactionShouldBeRejected() {
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Transaction unsigned = unsignedTransaction(
                alice.getAddress(), bob.getAddress(), 10, 1_000L
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> blockchain.addBlock(List.of(unsigned))
        );

        assertEquals(1, blockchain.getBlocks().size());
        assertTrue(blockchain.isValid());
    }

    @Test
    void modifyingTransactionWithoutUpdatingHashShouldInvalidateChain() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Blockchain blockchain = new Blockchain(DIFFICULTY, Map.of(alice.getAddress(), 10L));

        Transaction signed = sign(alice, bob, 10, 1_000L);
        blockchain.addBlock(List.of(signed));

        List<Block> tamperedBlocks =
                new ArrayList<>(blockchain.getBlocks());

        Block original = tamperedBlocks.get(1);

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
                original.hash()
        );

        tamperedBlocks.set(1, tampered);

        assertFalse(Blockchain.isValid(tamperedBlocks, DIFFICULTY));
        assertTrue(blockchain.isValid());
    }

    @Test
    void recalculatedAndReminedBlockWithInvalidSignatureShouldBeRejected() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet carol = new Wallet();

        Blockchain blockchain = new Blockchain(DIFFICULTY, Map.of(alice.getAddress(), 10L));

        Transaction signed = sign(alice, bob, 10, 1_000L);
        blockchain.addBlock(List.of(signed));

        List<Block> tamperedBlocks =
                new ArrayList<>(blockchain.getBlocks());

        Block original = tamperedBlocks.get(1);

        // Change the recipient but keep the original signature.
        Transaction tamperedTransaction = new Transaction(
                signed.sender(),
                carol.getAddress(),
                signed.amount(),
                signed.timestamp(),
                signed.senderPublicKey(),
                signed.signature()
        );

        // Recalculate the hash, then mine the modified block.
        Block candidate = new Block(
                original.index(),
                original.timestamp(),
                List.of(tamperedTransaction),
                original.previousHash(),
                0,
                null
        );

        Block remined = ProofOfWork.mine(candidate, DIFFICULTY);
        tamperedBlocks.set(1, remined);

        // Valid proof of work cannot make an invalid signature valid.
        assertTrue(ProofOfWork.isValid(remined, DIFFICULTY));
        assertFalse(Blockchain.isValid(tamperedBlocks, DIFFICULTY));
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
                null
        );

        assertFalse(ProofOfWork.isValid(recalculated, 64));
    }

    @Test
    void reconstructsBalancesByReplayingTransactionsInBlockOrder() {
        Wallet adam = new Wallet();
        Wallet eve = new Wallet();

        var initialBalance = Map.of(adam.getAddress(), 100L);
        Blockchain blockchain = new Blockchain(DIFFICULTY, initialBalance);

        Transaction first = adam.sign(
                new Transaction(adam.getAddress(), eve.getAddress(), 30L)
        );

        Transaction second = eve.sign(
                new Transaction(eve.getAddress(), adam.getAddress(), 10L)
        );

        // Adam's initial balance is supplied separately.
        blockchain.addBlock(List.of(first));
        blockchain.addBlock(List.of(second));

        LedgerState state = blockchain.buildLedgerState();

        assertEquals(80L, state.getBalance(adam.getAddress()));
        assertEquals(20L, state.getBalance(eve.getAddress()));
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
        return new Block(
                1,
                10_000L,
                transactions,
                "previous-hash",
                0,
                null
        );
    }
}
