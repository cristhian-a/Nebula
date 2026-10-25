package app.nebula.blockchain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TransactionTest {

    @Test
    void validSignatureShouldVerify() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Transaction unsigned = new Transaction(
                alice.getAddress(),
                bob.getAddress(),
                10
        );

        Transaction signed = alice.sign(unsigned);

        assertTrue(signed.verifySignature());
    }

    @Test
    void changingAmountShouldInvalidateSignature() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Transaction signed = alice.sign(new Transaction(
                alice.getAddress(),
                bob.getAddress(),
                10
        ));

        Transaction tampered = new Transaction(
                signed.sender(),
                signed.recipient(),
                20,
                signed.timestamp(),
                signed.senderPublicKey(),
                signed.signature()
        );

        assertFalse(tampered.verifySignature());
    }

    @Test
    void changingRecipientShouldInvalidateSignature() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet charlie = new Wallet();

        Transaction signed = alice.sign(new Transaction(
                alice.getAddress(),
                bob.getAddress(),
                10
        ));

        Transaction tampered = new Transaction(
                signed.sender(),
                charlie.getAddress(),
                signed.amount(),
                signed.timestamp(),
                signed.senderPublicKey(),
                signed.signature()
        );

        assertFalse(tampered.verifySignature());
    }

    @Test
    void walletShouldNotSignTransactionFromAnotherWallet() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Transaction transaction = new Transaction(
                alice.getAddress(),
                bob.getAddress(),
                10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> bob.sign(transaction)
        );
    }

    @Test
    void unsignedTransactionShouldNotVerify() {
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Transaction transaction = new Transaction(
                alice.getAddress(),
                bob.getAddress(),
                10
        );

        assertFalse(transaction.verifySignature());
    }
}
