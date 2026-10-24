
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
    void addingBlocksShouldPreserveValidity() {
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        blockchain.addBlock(List.of(
                new Transaction("Alice", "Bob", 10)
        ));

        blockchain.addBlock(List.of(
                new Transaction("Bob", "Carol", 3),
                new Transaction("Alice", "Carol", 5)
        ));

        assertEquals(3, blockchain.getBlocks().size());
        assertTrue(blockchain.isValid());
    }

    @Test
    void modifyingTransactionWithoutUpdatingHashShouldInvalidateChain() {
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        blockchain.addBlock(List.of(
                new Transaction("Alice", "Bob", 10)
        ));

        List<Block> tamperedBlocks =
                new ArrayList<>(blockchain.getBlocks());

        Block original = tamperedBlocks.get(1);

        Transaction tamperedTransaction =
                new Transaction(
                        "Alice",
                        "Bob",
                        999,
                        original.transactions().getFirst().timestamp()
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
    void changingTransactionShouldChangeBlockHash() {
        Transaction original =
                new Transaction("Alice", "Bob", 10, 1_000L);

        Transaction modified =
                new Transaction("Alice", "Bob", 11, 1_000L);

        Block originalBlock = createBlock(List.of(original));
        Block modifiedBlock = createBlock(List.of(modified));

        assertNotEquals(originalBlock.hash(), modifiedBlock.hash());
    }

    @Test
    void reorderingTransactionsShouldChangeBlockHash() {
        Transaction first =
                new Transaction("Alice", "Bob", 10, 1_000L);

        Transaction second =
                new Transaction("Bob", "Carol", 5, 2_000L);

        Block originalBlock = createBlock(List.of(first, second));
        Block reorderedBlock = createBlock(List.of(second, first));

        assertNotEquals(originalBlock.hash(), reorderedBlock.hash());
    }

    @Test
    void blockShouldNotBeAffectedByChangesToOriginalTransactionList() {
        List<Transaction> transactions = new ArrayList<>();

        transactions.add(new Transaction("Alice", "Bob", 10, 1_000L));

        Block block = createBlock(transactions);
        String originalHash = block.hash();

        transactions.add(new Transaction("Bob", "Carol", 5, 2_000L));

        assertEquals(1, block.transactions().size());
        assertEquals(originalHash, block.hash());
        assertTrue(block.hash().equals(block.calculateHash()));
    }

    @Test
    void blockTransactionListShouldBeImmutable() {
        Block block = createBlock(List.of(
                new Transaction("Alice", "Bob", 10, 1_000L)
        ));

        assertThrows(
                UnsupportedOperationException.class,
                () -> block.transactions().add(
                        new Transaction("Bob", "Carol", 5, 2_000L)
                )
        );
    }

    @Test
    void blockWithRecalculatedHashButInsufficientProofOfWorkShouldBeInvalid() {
        Block original = createBlock(List.of(
                new Transaction("Alice", "Bob", 10, 1_000L)
        ));

        Block recalculated = new Block(
                original.index(),
                original.timestamp(),
                List.of(new Transaction("Alice", "Bob", 999, 1_000L)),
                original.previousHash(),
                original.nonce(),
                null
        );

        assertFalse(ProofOfWork.isValid(recalculated, 64));
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
