package app.nebula.network;

import app.nebula.blockchain.LedgerState;
import app.nebula.blockchain.Transaction;
import app.nebula.blockchain.TransactionPool;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class TransactionRelay {
    private final NetworkNode node;
    private final TransactionPool pool;
    private final Supplier<LedgerState> ledgerSupplier;

    private final Set<String> knownTransactions = ConcurrentHashMap.newKeySet();

    public TransactionRelay(NetworkNode node, TransactionPool pool, Supplier<LedgerState> ledgerSupplier) {
        this.node = Objects.requireNonNull(node);
        this.pool = Objects.requireNonNull(pool);
        this.ledgerSupplier = Objects.requireNonNull(ledgerSupplier);
    }

    public boolean publish(Transaction transaction) {
        Objects.requireNonNull(transaction);

        if (!transaction.verifySignature())
            throw new IllegalArgumentException("Invalid transaction signature.");

        String id = TransactionCodec.transactionId(transaction);
        if (knownTransactions.contains(id)) return false;

        boolean accepted = pool.add(transaction, ledgerSupplier.get());
        if (!accepted) return false;

        knownTransactions.add(id);
        node.broadcast("TX|" + TransactionCodec.encode(transaction));

        return true;
    }

    public void handleMessage(String message) {
        if (message == null || !message.startsWith("TX|"))
            return;

        try {
            String encoded = message.substring(3);
            Transaction transaction = TransactionCodec.decode(encoded);

            String id = TransactionCodec.transactionId(transaction);

            if (!knownTransactions.add(id))
                return;

            boolean accepted = pool.add(transaction, ledgerSupplier.get());

            if (!accepted) {
                knownTransactions.remove(id);
                return;
            }

            node.broadcast(message);
        } catch (IllegalArgumentException _) {
        }
    }
}
