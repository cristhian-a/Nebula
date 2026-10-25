package app.nebula;

import app.nebula.blockchain.Block;
import app.nebula.blockchain.Blockchain;
import app.nebula.blockchain.Transaction;
import app.nebula.blockchain.Wallet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class Main {

    static void main() {
        var alice = new Wallet();
        var bob = new Wallet();
        var carol = new Wallet();
        var joe = new Wallet();
        var jannet = new Wallet();
        var will = new Wallet();

        Blockchain blockchain = new Blockchain(1);

        var t1 = alice.sign(new Transaction(alice.getAddress(), bob.getAddress(), 10));
        var t2 = bob.sign(new Transaction(bob.getAddress(), carol.getAddress(), 3));

        var t3 = joe.sign(new Transaction(joe.getAddress(), jannet.getAddress(), 5));
        var t4 = will.sign(new Transaction(will.getAddress(), jannet.getAddress(), 4));

        var block1 = blockchain.addBlock(List.of(t1, t2));
        printBlock(block1);

        var block2 = blockchain.addBlock(List.of(t3, t4));
        printBlock(block2);
        IO.println();

        for (Block block : blockchain.getBlocks()) {
            printBlock(block);
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
