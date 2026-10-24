package app.nebula;

import app.nebula.blockchain.Block;
import app.nebula.blockchain.Blockchain;

public class Main {

    static void main() {
        Blockchain blockchain = new Blockchain();

        blockchain.addBlock("Alice sends Bob 10 coins");
        blockchain.addBlock("Bob sends Carol 3 coins");

        for (Block block : blockchain.getBlocks()) {
            IO.println(block);
            IO.println();
        }

        IO.println("Blockchain valid: " + blockchain.isValid());
    }
}
