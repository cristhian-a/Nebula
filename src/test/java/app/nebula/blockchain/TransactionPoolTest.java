
package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TransactionPoolTest {

    @Test
    void shouldAcceptValidAffordableTransaction() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        LedgerState ledger = fundedLedger(alice, 1_000L);
        TransactionPool pool = new TransactionPool();

        Transaction transaction = sign(alice, bob, 500L);

        assertTrue(pool.add(transaction, ledger));
        assertEquals(1, pool.size());
        assertEquals(List.of(transaction), pool.getTransactions());

        // Pending transactions must not alter confirmed balances.
        assertEquals(1_000L, ledger.getBalance(alice.getAddress()));
    }

    @Test
    void shouldRejectInvalidSignature() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        LedgerState ledger = fundedLedger(alice, 1_000L);
        TransactionPool pool = new TransactionPool();

        Transaction signed = sign(alice, bob, 500L);

        Transaction tampered = new Transaction(
                signed.sender(),
                signed.recipient(),
                600L,
                signed.timestamp(),
                signed.senderPublicKey(),
                signed.signature());

        assertThrows(
                IllegalArgumentException.class,
                () -> pool.add(tampered, ledger));

        assertTrue(pool.isEmpty());
    }

    @Test
    void shouldRejectTransactionWithInsufficientBalance() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        LedgerState ledger = fundedLedger(alice, 100L);
        TransactionPool pool = new TransactionPool();

        Transaction transaction = sign(alice, bob, 101L);

        assertThrows(
                IllegalArgumentException.class,
                () -> pool.add(transaction, ledger));

        assertTrue(pool.isEmpty());
    }

    @Test
    void shouldRejectDuplicateTransaction() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        LedgerState ledger = fundedLedger(alice, 1_000L);
        TransactionPool pool = new TransactionPool();

        Transaction transaction = sign(alice, bob, 500L);

        assertTrue(pool.add(transaction, ledger));
        assertFalse(pool.add(transaction, ledger));

        assertEquals(1, pool.size());
    }

    @Test
    void shouldRemoveConfirmedTransactions() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet carol = new Wallet();

        LedgerState ledger = fundedLedger(alice, 2_000L);
        TransactionPool pool = new TransactionPool();

        Transaction first = sign(alice, bob, 500L);
        Transaction second = sign(alice, carol, 300L);

        pool.add(first, ledger);
        pool.add(second, ledger);

        pool.remove(List.of(first));

        assertEquals(1, pool.size());
        assertEquals(List.of(second), pool.getTransactions());
    }

    @Test
    void shouldReturnAnImmutableTransactionList() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        LedgerState ledger = fundedLedger(alice, 1_000L);
        TransactionPool pool = new TransactionPool();

        Transaction transaction = sign(alice, bob, 500L);
        pool.add(transaction, ledger);

        assertThrows(
                UnsupportedOperationException.class,
                () -> pool.getTransactions().clear());

        assertEquals(1, pool.size());
    }

    private static LedgerState fundedLedger(Wallet wallet, long amount) {
        LedgerState ledger = new LedgerState();
        ledger.applyReward(new MiningReward(wallet.getAddress(), amount));
        return ledger;
    }

    private static Transaction sign(
            Wallet sender,
            Wallet recipient,
            long amount) {

        return sender.sign(
                new Transaction(
                        sender.getAddress(),
                        recipient.getAddress(),
                        amount));
    }
}
