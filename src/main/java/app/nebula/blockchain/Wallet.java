package app.nebula.blockchain;

import java.security.*;
import java.util.HexFormat;
import java.util.Objects;

public final class Wallet {

    private final KeyPair keyPair;

    public Wallet() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            this.keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm not available.", e);
        }
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public String getAddress() {
        byte[] encodedPublicKey = Objects.requireNonNull(getPublicKey().getEncoded());
        return HexFormat.of().formatHex(encodedPublicKey);
    }

    public Transaction sign(Transaction transaction) {
        Objects.requireNonNull(transaction);

        if (!transaction.sender().equals(getAddress()))
            throw new IllegalArgumentException("Cannot sign a transaction from another wallet.");

        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(keyPair.getPrivate());
            signer.update(transaction.signingBytes());

            byte[] signature = signer.sign();

            return transaction.withSignature(getPublicKey(), signature);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
            throw new IllegalStateException("Could not sign transaction.", ex);
        }
    }
}
