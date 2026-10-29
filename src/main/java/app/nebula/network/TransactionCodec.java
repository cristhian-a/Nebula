package app.nebula.network;

import app.nebula.blockchain.Transaction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

public final class TransactionCodec {
    private static final int VERSION = 1;
    private static final int MAX_STRING_BYTES = 4_096;
    private static final int MAX_KEY_BYTES = 4_096;
    private static final int MAX_SIGNATURE_BYTES = 4_096;
    private static final int MAX_TRANSACTION_BYTES = 16_384;

    private TransactionCodec() {}

    public static String encode(Transaction transaction) {
        if (transaction == null || !transaction.verifySignature())
            throw new IllegalArgumentException("Cannot encode an invalid signature.");

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(VERSION);
                writeString(out, transaction.sender());
                writeString(out, transaction.recipient());
                out.writeLong(transaction.amount());
                out.writeLong(transaction.timestamp());

                writeBytes(out, transaction.senderPublicKey().getEncoded(), MAX_KEY_BYTES);
                writeBytes(out, transaction.signature(), MAX_SIGNATURE_BYTES);
            }

            byte[] encoded = bytes.toByteArray();

            if (encoded.length > MAX_TRANSACTION_BYTES)
                throw new IllegalArgumentException("Transaction is too large");

            return Base64.getEncoder().encodeToString(encoded);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Transaction decode(String encoded) {
        if (encoded == null || encoded.isBlank())
            throw new IllegalArgumentException("Encoded transaction cannot be blank.");

        final byte[] bytes;

        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid transaction encoding.", ex);
        }

        if (bytes.length > MAX_TRANSACTION_BYTES)
            throw new IllegalArgumentException("Transaction is too large");

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != VERSION)
                throw new IllegalArgumentException("Unsupported transaction version.");

            String sender = readString(in);
            String recipient = readString(in);
            long amount = in.readLong();
            long timestamp = in.readLong();

            byte[] publicKeyBytes = readBytes(in, MAX_KEY_BYTES);
            byte[] signature = readBytes(in, MAX_SIGNATURE_BYTES);

            if (in.available() != 0)
                throw new IllegalArgumentException("Unexpected trailing transaction data.");

            var publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Transaction transaction = new Transaction(
                    sender,
                    recipient,
                    amount,
                    timestamp,
                    publicKey,
                    signature
            );

            if (!transaction.verifySignature())
                throw new IllegalArgumentException("Invalid transaction signature.");

            return transaction;
        } catch (IOException |GeneralSecurityException ex) {
            throw new IllegalArgumentException("Could not decode transaction.", ex);
        }
    }

    public static String transactionId(Transaction transaction) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encode(transaction));
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        writeBytes(out, value.getBytes(StandardCharsets.UTF_8), MAX_STRING_BYTES);
    }

    private static String readString(DataInputStream inputStream) throws IOException {
        return new String(readBytes(inputStream, MAX_STRING_BYTES), StandardCharsets.UTF_8);
    }

    private static void writeBytes(DataOutputStream out, byte[] bytes, int maximum) throws IOException {
        if (bytes == null || bytes.length > maximum)
            throw new IllegalArgumentException("Invalid field length");

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static byte[] readBytes(DataInputStream inputStream, int maximum) throws IOException {
        int length = inputStream.readInt();

        if (length < 0 || length > maximum || length > inputStream.available())
            throw new IllegalArgumentException("Invalid field length");

        return inputStream.readNBytes(length);
    }
}
