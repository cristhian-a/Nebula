
package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class LedgerStateTest {

    @Test
    void appliesValidTransactionAndUpdatesBalances() {
        Wallet adam = new Wallet();
        Wallet eve = new Wallet();

        LedgerState state = new LedgerState(
                Map.of(adam.getAddress(), 100L)
        );

        Transaction transaction = adam.sign(
                new Transaction(adam.getAddress(), eve.getAddress(), 30L)
        );

        state.apply(transaction);

        assertEquals(70L, state.getBalance(adam.getAddress()));
        assertEquals(30L, state.getBalance(eve.getAddress()));
    }


    @Test
    void rejectsTransactionWhenBalanceIsInsufficient() {
        Wallet adam = new Wallet();
        Wallet eve = new Wallet();

        LedgerState state = new LedgerState(
                Map.of(adam.getAddress(), 20L)
        );

        Transaction transaction = adam.sign(
                new Transaction(adam.getAddress(), eve.getAddress(), 30L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> state.apply(transaction)
        );

        assertEquals(20L, state.getBalance(adam.getAddress()));
        assertEquals(0L, state.getBalance(eve.getAddress()));
    }
}
