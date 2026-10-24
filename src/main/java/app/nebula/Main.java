package app.nebula;

import app.nebula.blockchain.Block;
import app.nebula.blockchain.Blockchain;

public class Main {

    static void main() {
        Blockchain blockchain = new Blockchain(5);

        var block1 = blockchain.addBlock("Alice sends Bob 10 coins");
        IO.println("Block mined: " + block1.toString());

        var block2 = blockchain.addBlock("Bob sends Carol 3 coins");
        IO.println("Block mined: " + block2.toString());
        IO.println();

        for (Block block : blockchain.getBlocks()) {
            IO.println(block);
            IO.println();
        }

        IO.println("Blockchain valid: " + blockchain.isValid());
    }
}
