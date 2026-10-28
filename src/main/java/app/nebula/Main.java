
package app.nebula;

import app.nebula.blockchain.Block;
import app.nebula.blockchain.Blockchain;
import app.nebula.blockchain.LedgerState;
import app.nebula.blockchain.Miner;
import app.nebula.blockchain.Transaction;
import app.nebula.blockchain.TransactionPool;
import app.nebula.blockchain.Wallet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Main {

    private static final int DIFFICULTY = 5;

    private static final int MAX_PARTICIPANTS = 25;
    private static final int MAX_RECENT_EVENTS = 8;

    private static final long MIN_TRANSACTION_CENTS = 100L;
    private static final long MAX_TRANSACTION_CENTS = 2_000L;

    private static final int NEWCOMER_CHANCE_PERCENT = 12;
    private static final int TRANSACTION_CHANCE_PERCENT = 55;
    private static final int NEWCOMER_FUNDING_CHANCE_PERCENT = 70;

    private static final long MIN_FUNDING_CENTS = 500L;
    private static final long MAX_FUNDING_CENTS = 2_000L;

    private static final long LOOP_DELAY_MILLIS = 200L;

    private static final Random RANDOM = new Random();

    private static final Map<String, Wallet> ACCOUNTS =
            new LinkedHashMap<>();

    private static final Deque<String> RECENT_EVENTS =
            new ArrayDeque<>();

    private static int nextParticipantId = 1;

    private static boolean interactiveConsole;

    static void main() {
        Blockchain blockchain = new Blockchain(DIFFICULTY);
        TransactionPool transactionPool = new TransactionPool();

        Wallet minerWallet = new Wallet();

        ACCOUNTS.put("Miner", minerWallet);

        Miner miner = new Miner(
                blockchain,
                transactionPool,
                minerWallet.getAddress()
        );

        interactiveConsole = System.console() != null;

        addEvent("Nebula simulation initialized.");
        addEvent("Initial supply: 0.00 coins.");

        printStartup();

        try {
            // Bootstrap: the first mined block creates the first coins.
            Block bootstrapBlock = miner.minePendingTransactions();

            addEvent("Bootstrap block mined: #"
                    + bootstrapBlock.index());

            addEvent("Miner received "
                    + formatCoins(
                    bootstrapBlock.miningReward().amount()
            )
                    + " coins.");

            while (!Thread.currentThread().isInterrupted()) {

                // Mine pending transactions, or mine an empty block.
                Block block = miner.minePendingTransactions();

                addEvent("Block #" + block.index()
                        + " mined: "
                        + block.transactions().size()
                        + " transaction(s), nonce "
                        + block.nonce() + ".");

                LedgerState ledger = blockchain.buildLedger();

                // Simulate economic activity between blocks.
                simulateNewcomer(
                        transactionPool,
                        ledger,
                        minerWallet
                );

                ledger = blockchain.buildLedger();

                simulateRandomTransaction(
                        transactionPool,
                        ledger
                );

                printDashboard(
                        blockchain,
                        transactionPool,
                        minerWallet
                );

                Thread.sleep(LOOP_DELAY_MILLIS);
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printStartup() {
        IO.println("============================================");
        IO.println("           NEBULA BLOCKCHAIN");
        IO.println("           LIVE SIMULATION");
        IO.println("============================================");
        IO.println("Difficulty: " + DIFFICULTY);
        IO.println("Initial supply: 0.00 coins");
        IO.println("Initial participants: 1 (Miner)");
        IO.println("Maximum participants: " + MAX_PARTICIPANTS);
        IO.println("Stop with Ctrl+C.");
        IO.println();

        if (interactiveConsole) {
            IO.println("Live console refresh enabled.");
        } else {
            IO.println("Console clearing unavailable; "
                    + "printing successive snapshots.");
        }

        IO.println();
        IO.println("Starting bootstrap mining...");
    }

    private static void simulateNewcomer(
            TransactionPool pool,
            LedgerState ledger,
            Wallet minerWallet
    ) {
        if (ACCOUNTS.size() >= MAX_PARTICIPANTS) {
            return;
        }

        if (RANDOM.nextInt(100) >= NEWCOMER_CHANCE_PERCENT) {
            return;
        }

        String name = "User-" + nextParticipantId++;
        Wallet wallet = new Wallet();

        ACCOUNTS.put(name, wallet);

        addEvent(name + " joined Nebula.");

        if (RANDOM.nextInt(100)
                >= NEWCOMER_FUNDING_CHANCE_PERCENT) {
            addEvent(name + " joined without initial funds.");
            return;
        }

        // Only one unconfirmed outgoing transaction per sender.
        if (hasPendingOutgoing(pool, minerWallet.getAddress())) {
            addEvent(name + " is waiting for a funding transfer.");
            return;
        }

        long minerBalance = ledger.getBalance(
                minerWallet.getAddress()
        );

        if (minerBalance < MIN_FUNDING_CENTS) {
            addEvent(name + " joined; miner cannot fund them yet.");
            return;
        }

        long maximum = Math.min(
                MAX_FUNDING_CENTS,
                minerBalance
        );

        long amount = randomAmount(
                MIN_FUNDING_CENTS,
                maximum
        );

        Transaction transaction = minerWallet.sign(
                new Transaction(
                        minerWallet.getAddress(),
                        wallet.getAddress(),
                        amount
                )
        );

        try {
            if (pool.add(transaction, ledger)) {
                addEvent("Miner submitted "
                        + formatCoins(amount)
                        + " coins for " + name + ".");
            }
        } catch (IllegalArgumentException exception) {
            addEvent("Funding for " + name
                    + " was rejected: "
                    + exception.getMessage());
        }
    }

    private static void simulateRandomTransaction(
            TransactionPool pool,
            LedgerState ledger
    ) {
        if (RANDOM.nextInt(100)
                >= TRANSACTION_CHANCE_PERCENT) {
            return;
        }

        List<String> eligibleSenders = new ArrayList<>();

        for (Map.Entry<String, Wallet> entry
                : ACCOUNTS.entrySet()) {

            Wallet wallet = entry.getValue();

            long balance = ledger.getBalance(
                    wallet.getAddress()
            );

            if (balance >= MIN_TRANSACTION_CENTS
                    && !hasPendingOutgoing(
                    pool,
                    wallet.getAddress()
            )) {
                eligibleSenders.add(entry.getKey());
            }
        }

        if (eligibleSenders.isEmpty()) {
            return;
        }

        String senderName = eligibleSenders.get(
                RANDOM.nextInt(eligibleSenders.size())
        );

        Wallet sender = ACCOUNTS.get(senderName);

        List<String> possibleRecipients = new ArrayList<>(
                ACCOUNTS.keySet()
        );

        possibleRecipients.remove(senderName);

        if (possibleRecipients.isEmpty()) {
            return;
        }

        String recipientName = possibleRecipients.get(
                RANDOM.nextInt(possibleRecipients.size())
        );

        Wallet recipient = ACCOUNTS.get(recipientName);

        long balance = ledger.getBalance(
                sender.getAddress()
        );

        long maximum = Math.min(
                MAX_TRANSACTION_CENTS,
                balance
        );

        long amount = randomAmount(
                MIN_TRANSACTION_CENTS,
                maximum
        );

        Transaction transaction = sender.sign(
                new Transaction(
                        sender.getAddress(),
                        recipient.getAddress(),
                        amount
                )
        );

        try {
            if (pool.add(transaction, ledger)) {
                addEvent(senderName + " -> "
                        + recipientName + ": "
                        + formatCoins(amount)
                        + " coins submitted.");
            }
        } catch (IllegalArgumentException exception) {
            addEvent("Transaction rejected: "
                    + senderName + " -> "
                    + recipientName + ".");
        }
    }

    private static boolean hasPendingOutgoing(
            TransactionPool pool,
            String address
    ) {
        return pool.getTransactions()
                .stream()
                .anyMatch(transaction ->
                        transaction.sender().equals(address));
    }

    private static long randomAmount(
            long minimum,
            long maximum
    ) {
        if (maximum <= minimum) {
            return minimum;
        }

        return RANDOM.nextLong(minimum, maximum + 1);
    }

    private static void printDashboard(
            Blockchain blockchain,
            TransactionPool pool,
            Wallet minerWallet
    ) {
        if (interactiveConsole) {
            System.out.print("\u001B[H\u001B[2J");
            System.out.flush();
        }

        LedgerState ledger = blockchain.buildLedger();

        List<Block> blocks = blockchain.getBlocks();
        Block latestBlock = blocks.getLast();

        IO.println("==================================================");
        IO.println("              NEBULA BLOCKCHAIN");
        IO.println("                LIVE NETWORK");
        IO.println("==================================================");

        IO.println("Chain height:       " + latestBlock.index());
        IO.println("Total blocks:       " + blocks.size());
        IO.println("Participants:       " + ACCOUNTS.size());
        IO.println("Pending tx:         " + pool.size());

        IO.println("Total supply:       "
                + formatCoins(ledger.getTotalSupplyCents())
                + " coins");

        IO.println("Miner balance:      "
                + formatCoins(
                ledger.getBalance(minerWallet.getAddress())
        )
                + " coins");

        IO.println("Blockchain valid:   " + blockchain.isValid());

        IO.println();
        IO.println("---------------- LATEST BLOCK ----------------");

        IO.println("Block:              #" + latestBlock.index());
        IO.println("Timestamp:          "
                + formatTimestamp(latestBlock.timestamp()));
        IO.println("Transactions:       "
                + latestBlock.transactions().size());
        IO.println("Nonce:              " + latestBlock.nonce());
        IO.println("Previous hash:      "
                + shortAddress(latestBlock.previousHash()));
        IO.println("Hash:               "
                + shortAddress(latestBlock.hash()));

        if (latestBlock.miningReward() != null) {
            IO.println("Mining reward:      "
                    + formatCoins(
                    latestBlock.miningReward().amount()
            )
                    + " coins");
        }

        IO.println();
        IO.println("---------------- RECENT EVENTS ----------------");

        for (String event : RECENT_EVENTS) {
            IO.println("  " + event);
        }

        IO.println();
        IO.println("---------------- PENDING TX -------------------");

        List<Transaction> pending = pool.getTransactions();

        if (pending.isEmpty()) {
            IO.println("  No pending transactions.");
        } else {
            int displayed = 0;

            for (Transaction transaction : pending) {
                if (displayed >= 6) {
                    IO.println("  ... and "
                            + (pending.size() - displayed)
                            + " more.");
                    break;
                }

                IO.println("  "
                        + shortAddress(transaction.sender())
                        + " -> "
                        + shortAddress(transaction.recipient())
                        + " | "
                        + formatCoins(transaction.amount())
                        + " coins");

                displayed++;
            }
        }

        IO.println();
        IO.println("---------------- WALLET BALANCES --------------");

        int displayedAccounts = 0;

        for (Map.Entry<String, Wallet> entry
                : ACCOUNTS.entrySet()) {

            if (displayedAccounts >= 12) {
                IO.println("  ... and "
                        + (ACCOUNTS.size() - displayedAccounts)
                        + " more participants.");
                break;
            }

            long balance = ledger.getBalance(
                    entry.getValue().getAddress()
            );

            IO.println(String.format(
                    Locale.US,
                    "  %-12s %12s coins",
                    entry.getKey(),
                    formatCoins(balance)
            ));

            displayedAccounts++;
        }

        IO.println();
        IO.println("Mining continuously. Press Ctrl+C to stop.");
    }

    private static void addEvent(String event) {
        RECENT_EVENTS.addFirst(event);

        while (RECENT_EVENTS.size() > MAX_RECENT_EVENTS) {
            RECENT_EVENTS.removeLast();
        }
    }

    private static String formatTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );

        return dateTime.toString();
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
