package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class BlockchainTest {

    @Test
    void newBlockchainShouldBeValid() {
        Blockchain blockchain = new Blockchain(4);

        assertTrue(blockchain.isValid());
    }

    @Test
    void addingBlocksShouldPreserveValidity() {
        Blockchain blockchain = new Blockchain(4);

        blockchain.addBlock("First block");
        blockchain.addBlock("Second block");

        assertEquals(3, blockchain.getBlocks().size());
        assertTrue(blockchain.isValid());
    }

    @Test
    void modifyingBlockDataWithoutUpdatingHashShouldInvalidateChain() {
        final int DIFFICULTY = 4;
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        blockchain.addBlock("Original data");
        blockchain.addBlock("Another block");

        List<Block> tamperedBlocks =
                new ArrayList<>(blockchain.getBlocks());

        Block original = tamperedBlocks.get(1);

        Block tampered = new Block(
                original.index(),
                original.timestamp(),
                "Tampered data",
                original.previousHash(),
                original.nonce(),
                original.hash()
        );

        tamperedBlocks.set(1, tampered);

        assertFalse(Blockchain.isValid(tamperedBlocks, DIFFICULTY));
        assertTrue(blockchain.isValid());
    }
}
