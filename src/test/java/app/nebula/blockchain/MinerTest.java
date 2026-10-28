package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MinerTest {

    @Test
    void shouldMinePendingTransactionsAndRemoveThemFromPool() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet minerWallet = new Wallet();

        Blockchain blockchain = new Blockchain(1);
        blockchain.addBlock(List.of(), alice.getAddress());

        TransactionPool pool = new TransactionPool();
        LedgerState ledger = blockchain.buildLedger();

        Transaction transaction = sign(alice, bob, 500L);
        pool.add(transaction, ledger);

        Miner miner = new Miner(
                blockchain, pool, minerWallet.getAddress());

        Block minedBlock = miner.minePendingTransactions();

        assertTrue(minedBlock.transactions().contains(transaction));
        assertTrue(pool.isEmpty());

        LedgerState updatedLedger = blockchain.buildLedger();

        assertEquals(500L, updatedLedger.getBalance(bob.getAddress()));
        assertTrue(blockchain.isValid());
    }

    private static Transaction sign(Wallet sender, Wallet recipient, long amount) {
        return sender.sign(
                new Transaction(
                        sender.getAddress(),
                        recipient.getAddress(),
                        amount
                ));
    }
}
