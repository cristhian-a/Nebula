package app.nebula.blockchain;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class Hashing {

    private Hashing() {
    }

    public static String calculateHash(int index, long timestamp, List<Transaction> transactions, String previousHash, long nonce) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (DataOutputStream output = new DataOutputStream(buffer)) {
                output.writeInt(index);
                output.writeLong(timestamp);

                output.writeInt(transactions.size());

                for (var t : transactions) {
                    writeTransaction(output, t);
                }

                byte[] previousHashBytes = previousHash.getBytes(StandardCharsets.UTF_8);
                output.writeInt(previousHashBytes.length);
                output.write(previousHashBytes);

                output.writeLong(nonce);
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(buffer.toByteArray())
            );
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Could not serialize block data.", ex
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is not available.", ex
            );
        }
    }

    private static void writeTransaction(DataOutputStream output, Transaction transaction) throws IOException {
        byte[] senderBytes = transaction.sender().getBytes(StandardCharsets.UTF_8);
        output.writeInt(senderBytes.length);
        output.write(senderBytes);

        byte[] recipientBytes = transaction.recipient().getBytes(StandardCharsets.UTF_8);
        output.writeInt(recipientBytes.length);
        output.write(recipientBytes);

        output.writeLong(transaction.amount());
        output.writeLong(transaction.timestamp());

        byte[] publicKeyBytes = transaction.senderPublicKey() == null ? new byte[0]
                : transaction.senderPublicKey().getEncoded();
        output.writeInt(publicKeyBytes.length);
        output.write(publicKeyBytes);

        byte[] signatureBytes = transaction.signature() == null ? new byte[0]
                : transaction.signature();
        output.writeInt(signatureBytes.length);
        output.write(signatureBytes);
    }
}
