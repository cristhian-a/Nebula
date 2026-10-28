package app.nebula.blockchain;

import java.util.List;
import java.util.Objects;

public final class Miner {
    private final Blockchain blockchain;
    private final TransactionPool transactionPool;
    private final String minerAddress;

    public Miner(Blockchain blockchain, TransactionPool pool, String minerAddress) {
        this.blockchain = Objects.requireNonNull(blockchain);
        this.transactionPool = Objects.requireNonNull(pool);
        this.minerAddress = Objects.requireNonNull(minerAddress);

        if (this.minerAddress.isBlank())
            throw new IllegalArgumentException("Miner Address cannot be blank.");
    }

    public Block minePendingTransactions() {
        LedgerState confirmedLedger = blockchain.buildLedger();
        List<Transaction> selected = transactionPool.selectTransactionsForBlock(confirmedLedger);
        Block block = blockchain.addBlock(selected, minerAddress);
        transactionPool.remove(selected);
        return block;
    }
}
