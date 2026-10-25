package app.nebula.blockchain;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import java.util.Objects;

public record Transaction(String sender, String recipient, long amount, long timestamp,
                          PublicKey senderPublicKey, byte[] signature) {

    public Transaction(String sender, String recipient, long amount) {
        this(sender, recipient, amount, System.currentTimeMillis(), null, null);
    }

    public Transaction {
        Objects.requireNonNull(sender);
        Objects.requireNonNull(recipient);

        if (sender.isBlank())
            throw new IllegalArgumentException("Sender cannot be blank.");

        if (recipient.isBlank())
            throw new IllegalArgumentException("Recipient cannot be blank.");

        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be positive.");

        if ((senderPublicKey == null) != (signature == null)) {
            throw new IllegalArgumentException(
                    "Public key and signature must both be present or absent."
            );
        }

        signature = signature == null ? null : signature.clone();
    }

    @Override
    public byte[] signature() {
        return signature == null ? null : signature.clone();
    }

    public Transaction withSignature(PublicKey publicKey, byte[] signature) {
        return new Transaction(
                sender,
                recipient,
                amount,
                timestamp,
                Objects.requireNonNull(publicKey),
                Objects.requireNonNull(signature)
        );
    }

    public byte[] signingBytes() {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (DataOutputStream out = new DataOutputStream(buffer)) {
                writeString(out, "nebula-transaction-v1");
                writeString(out, sender);
                writeString(out, recipient);
                out.writeLong(amount);
                out.writeLong(timestamp);
            }

            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not serialize transaction for signing.", ex);
        }
    }

    public boolean verifySignature() {
        if (senderPublicKey == null || signature == null)
            return false;

        String publicKeyAddress = HexFormat.of().formatHex(senderPublicKey.getEncoded());

        if (!sender.equals(publicKeyAddress))
            return false;

        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(senderPublicKey);
            verifier.update(signingBytes());

            return verifier.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
            return false;
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
