package app.nebula.blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TransactionPool {
    private final List<Transaction> transactions = new ArrayList<>();

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }

    public int size() {
        return transactions.size();
    }

    public void clear() {
        transactions.clear();
    }

    public void remove(List<Transaction> transactions) {
        Objects.requireNonNull(transactions);
        this.transactions.removeAll(transactions);
    }

    public boolean add(Transaction transaction, LedgerState confirmedLedger) {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(confirmedLedger);

        if (!transaction.verifySignature())
            throw new IllegalArgumentException("Invalid signature.");

        if (transactions.contains(transaction))
            return false;

        long balance = confirmedLedger.getBalance(transaction.sender());
        if (balance < transaction.amount())
            throw new IllegalArgumentException("Insufficient funds for transaction");

        transactions.add(transaction);
        return true;
    }

    public List<Transaction> selectTransactionsForBlock(LedgerState confirmedLedger) {
        Objects.requireNonNull(confirmedLedger);

        LedgerState candidateState = new LedgerState(confirmedLedger);
        List<Transaction> selected = new ArrayList<>();

        for (Transaction t : transactions) {
            try {
                candidateState.apply(t);
                selected.add(t);
            } catch (IllegalStateException | ArithmeticException _) {
            }
        }

        return List.copyOf(selected);
    }
}
