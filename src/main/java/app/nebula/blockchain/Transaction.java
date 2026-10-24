package app.nebula.blockchain;

import java.util.Objects;

public record Transaction(String sender, String recipient, long amount, long timestamp) {

    public Transaction(String sender, String recipient, long amount) {
        this(sender, recipient, amount, System.currentTimeMillis());
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
    }

}
