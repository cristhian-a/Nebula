
package app.nebula.network;

import app.nebula.blockchain.Blockchain;
import app.nebula.blockchain.Miner;
import app.nebula.blockchain.Transaction;
import app.nebula.blockchain.TransactionPool;
import app.nebula.blockchain.Wallet;

final class TransactionNetworkDemo {

    static void main() throws Exception {
        Blockchain chainA = new Blockchain(1);
        Blockchain chainB = new Blockchain(1);

        TransactionPool poolA = new TransactionPool();
        TransactionPool poolB = new TransactionPool();

        Wallet minerA = new Wallet();
        Wallet minerB = new Wallet();

        chainA.addBlock(
                java.util.List.of(),
                minerA.getAddress()
        );

        chainB.addBlock(
                java.util.List.of(),
                minerB.getAddress()
        );

        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        TransactionRelay[] relays = new TransactionRelay[2];

        try (
                NetworkNode nodeA = new NetworkNode(
                        8_001,
                        message -> IO.println("[Node A] " + message),
                        message -> relays[0].handleMessage(message)
                );
                NetworkNode nodeB = new NetworkNode(
                        8_002,
                        message -> IO.println("[Node B] " + message),
                        message -> relays[1].handleMessage(message)
                )
        ) {
            relays[0] = new TransactionRelay(
                    nodeA,
                    poolA,
                    chainA::buildLedger
            );

            relays[1] = new TransactionRelay(
                    nodeB,
                    poolB,
                    chainB::buildLedger
            );

            nodeA.start();
            nodeB.start();

            nodeA.connectTo("127.0.0.1", nodeB.getPort());

            // Give the connection time to become active.
            Thread.sleep(300);

            // Fund Alice on both chains for this isolated
            // transaction-propagation test.
            chainA.addBlock(
                    java.util.List.of(
                            minerA.sign(new Transaction(
                                    minerA.getAddress(),
                                    alice.getAddress(),
                                    1_000L
                            ))
                    ),
                    minerA.getAddress()
            );

            chainB.addBlock(
                    java.util.List.of(
                            minerB.sign(new Transaction(
                                    minerB.getAddress(),
                                    alice.getAddress(),
                                    1_000L
                            ))
                    ),
                    minerB.getAddress()
            );

            Transaction payment = alice.sign(
                    new Transaction(
                            alice.getAddress(),
                            bob.getAddress(),
                            250L
                    )
            );

            IO.println();
            IO.println("Publishing transaction on Node A...");

            relays[0].publish(payment);

            // Wait for network delivery.
            Thread.sleep(500);

            IO.println();
            IO.println("Node A mempool: " + poolA.size());
            IO.println("Node B mempool: " + poolB.size());

            IO.println(
                    "Node B received transaction: "
                            + poolB.getTransactions().stream()
                            .anyMatch(tx ->
                                    TransactionCodec.transactionId(tx)
                                            .equals(
                                                    TransactionCodec
                                                            .transactionId(payment)
                                            ))
            );

            IO.println("Node A chain valid: "
                    + chainA.isValid());

            IO.println("Node B chain valid: "
                    + chainB.isValid());
        }
    }
}
