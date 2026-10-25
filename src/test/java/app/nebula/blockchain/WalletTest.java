package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WalletTest {

    @Test
    void creatingWalletShouldGeneratePublicKeyAndAddress() {
        Wallet wallet = new Wallet();

        assertNotNull(wallet.getPublicKey());
        assertNotNull(wallet.getAddress());
        assertFalse(wallet.getAddress().isBlank());
    }

    @Test
    void differentWalletsShouldHaveDifferentAddresses() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        assertNotEquals(alice.getAddress(), bob.getAddress());
    }

    @Test
    void addressShouldBeHexadecimal() {
        Wallet wallet = new Wallet();

        assertTrue(wallet.getAddress().matches("[0-9a-f]+"));
    }
}
