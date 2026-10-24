package app.nebula.blockchain;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hashing {

    private Hashing() {
    }

    public static String calculateHash(int index, long timestamp, String data, String previousHash, long nonce) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (DataOutputStream output = new DataOutputStream(buffer)) {
                output.writeInt(index);
                output.writeLong(timestamp);

                byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
                output.writeInt(dataBytes.length);
                output.write(dataBytes);

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
}
