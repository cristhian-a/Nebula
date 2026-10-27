
package app.nebula;

import app.nebula.blockchain.Block;
import app.nebula.blockchain.Blockchain;
import app.nebula.blockchain.LedgerState;
import app.nebula.blockchain.Transaction;
import app.nebula.blockchain.Wallet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main {

    private static final int DIFFICULTY = 4;

    static void main() {
        Wallet miner = new Wallet();
        Blockchain blockchain = new Blockchain(DIFFICULTY);

        IO.println("========================================");
        IO.println("             NEBULA BLOCKCHAIN");
        IO.println("========================================");
        IO.println("Difficulty: " + DIFFICULTY);
        IO.println("Initial supply: 0.00 coins");
        IO.println("Miner initial balance: 0.00 coins");
        IO.println();

        // Phase 1: Mine without any initial supply or transactions.
        IO.println("PHASE 1 - BOOTSTRAP MINING");
        IO.println("Mining the first block with an empty ledger...");

        Block block1 = blockchain.addBlock(
                List.of(),
                miner.getAddress()
        );

        printBlock(block1);
        printSupply(blockchain);

        IO.println("Miner balance after first block: "
                + formatCoins(blockchain.buildLedger()
                .getBalance(miner.getAddress()))
                + " coins");

        // Phase 2: Introduce wallets after mining has created coins.
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet carol = new Wallet();
        Wallet joe = new Wallet();
        Wallet jannet = new Wallet();
        Wallet will = new Wallet();

        Map<String, String> accounts = Map.of(
                "Miner", miner.getAddress(),
                "Alice", alice.getAddress(),
                "Bob", bob.getAddress(),
                "Carol", carol.getAddress(),
                "Joe", joe.getAddress(),
                "Jannet", jannet.getAddress(),
                "Will", will.getAddress()
        );

        IO.println();
        IO.println("PHASE 2 - FIRST TRANSFERS");
        IO.println("The miner sends Alice 100.00 coins.");

        Transaction t1 = miner.sign(
                new Transaction(
                        miner.getAddress(),
                        alice.getAddress(),
                        10_000L
                )
        );

        Block block2 = blockchain.addBlock(
                List.of(t1),
                miner.getAddress()
        );

        printBlock(block2);

        IO.println();
        IO.println("PHASE 3 - WALLET TRANSACTIONS");
        IO.println("Alice -> Bob: 10.00 coins");
        IO.println("Bob -> Carol: 3.00 coins");

        Transaction t2 = alice.sign(
                new Transaction(
                        alice.getAddress(),
                        bob.getAddress(),
                        1_000L
                )
        );

        Transaction t3 = bob.sign(
                new Transaction(
                        bob.getAddress(),
                        carol.getAddress(),
                        300L
                )
        );

        Block block3 = blockchain.addBlock(
                List.of(t2, t3),
                miner.getAddress()
        );

        printBlock(block3);

        IO.println();
        IO.println("PHASE 4 - FUNDING MORE WALLETS");
        IO.println("Miner -> Joe: 20.00 coins");
        IO.println("Miner -> Will: 20.00 coins");

        Transaction t4 = miner.sign(
                new Transaction(
                        miner.getAddress(),
                        joe.getAddress(),
                        2_000L
                )
        );

        Transaction t5 = miner.sign(
                new Transaction(
                        miner.getAddress(),
                        will.getAddress(),
                        2_000L
                )
        );

        Block block4 = blockchain.addBlock(
                List.of(t4, t5),
                miner.getAddress()
        );

        printBlock(block4);

        IO.println();
        IO.println("PHASE 5 - MORE TRANSACTIONS");
        IO.println("Joe -> Jannet: 5.00 coins");
        IO.println("Will -> Jannet: 4.00 coins");

        Transaction t6 = joe.sign(
                new Transaction(
                        joe.getAddress(),
                        jannet.getAddress(),
                        500L
                )
        );

        Transaction t7 = will.sign(
                new Transaction(
                        will.getAddress(),
                        jannet.getAddress(),
                        400L
                )
        );

        Block block5 = blockchain.addBlock(
                List.of(t6, t7),
                miner.getAddress()
        );

        printBlock(block5);

        // Final report.
        IO.println();
        IO.println("========================================");
        IO.println("             BLOCKCHAIN");
        IO.println("========================================");

        for (Block block : blockchain.getBlocks()) {
            printBlock(block);
            IO.println();
        }

        LedgerState finalState = blockchain.buildLedger();

        IO.println("========================================");
        IO.println("            FINAL BALANCES");
        IO.println("========================================");

        printBalances(finalState, accounts);

        IO.println();
        IO.println("Total supply: "
                + formatCoins(finalState.getTotalSupplyCents())
                + " coins");

        IO.println("Blockchain valid: " + blockchain.isValid());
    }

    private static void printBlock(Block block) {
        LocalDateTime minedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(block.timestamp()),
                ZoneId.systemDefault()
        );

        IO.println("----------------------------------------");
        IO.println("Block #" + block.index());
        IO.println("Timestamp: " + minedAt);
        IO.println("Previous hash: " + block.previousHash());
        IO.println("Hash: " + block.hash());
        IO.println("Nonce: " + block.nonce());
        IO.println("Transactions: " + block.transactions().size());

        for (Transaction transaction : block.transactions()) {
            IO.println("  " + shortAddress(transaction.sender())
                    + " -> " + shortAddress(transaction.recipient())
                    + " | " + formatCoins(transaction.amount())
                    + " coins");
        }

        if (block.miningReward() != null) {
            IO.println("Mining reward: "
                    + formatCoins(block.miningReward().amount())
                    + " coins -> "
                    + shortAddress(block.miningReward().recipient()));
        } else {
            IO.println("Mining reward: none (genesis block)");
        }
    }

    private static void printSupply(Blockchain blockchain) {
        long supply = blockchain.buildLedger().getTotalSupplyCents();

        IO.println("Current total supply: "
                + formatCoins(supply) + " coins");
    }

    private static void printBalances(
            LedgerState ledger,
            Map<String, String> accounts
    ) {
        accounts.forEach((name, address) ->
                IO.println("  " + name + ": "
                        + formatCoins(ledger.getBalance(address))
                        + " coins"));
    }

    private static String formatCoins(long cents) {
        return String.format(
                Locale.US,
                "%,d.%02d",
                cents / 100,
                Math.abs(cents % 100)
        );
    }

    private static String shortAddress(String address) {
        if (address == null || address.length() <= 16) {
            return address;
        }

        return address.substring(0, 8)
                + "..."
                + address.substring(address.length() - 6);
    }
}
