package app.nebula;

import app.nebula.blockchain.Block;
import app.nebula.blockchain.Blockchain;
import app.nebula.blockchain.Transaction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class Main {

    static void main() {
        Blockchain blockchain = new Blockchain(7);

        var t1 = new Transaction("Alice", "Bob", 10);
        var t2 = new Transaction("Bob", "Carol", 3);

        var t3 = new Transaction("Joe", "Jannet", 5);
        var t4 = new Transaction("Will", "Jannet", 4);

        var block1 = blockchain.addBlock(List.of(t1, t2));
        printBlock(block1);

        var block2 = blockchain.addBlock(List.of(t3, t4));
        printBlock(block2);
        IO.println();

        for (Block block : blockchain.getBlocks()) {
            IO.println(block);
            IO.println();
        }

        IO.println("Blockchain valid: " + blockchain.isValid());
    }

    private static void printBlock(Block b) {
        IO.println("Block mined at " +
                LocalDateTime.ofInstant(Instant.ofEpochMilli(b.timestamp()), ZoneId.systemDefault()) +
                ": " + b);
    }
}
