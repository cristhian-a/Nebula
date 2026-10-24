package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class BlockchainTest {

    @Test
    void newBlockchainShouldBeValid() {
        Blockchain blockchain = new Blockchain();

        assertTrue(blockchain.isValid());
    }

    @Test
    void addingBlocksShouldPreserveValidity() {
        Blockchain blockchain = new Blockchain();

        blockchain.addBlock("First block");
        blockchain.addBlock("Second block");

        assertEquals(3, blockchain.getBlocks().size());
        assertTrue(blockchain.isValid());
    }

    @Test
    void modifyingBlockDataWithoutUpdatingHashShouldInvalidateChain() {
        Blockchain blockchain = new Blockchain();

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
                original.hash()
        );

        tamperedBlocks.set(1, tampered);

        assertFalse(Blockchain.isValid(tamperedBlocks));
        assertTrue(blockchain.isValid());
    }
}
