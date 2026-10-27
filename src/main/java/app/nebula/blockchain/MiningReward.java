package app.nebula.blockchain;

import java.util.Objects;

public record MiningReward(String recipient, long amount) {

    public MiningReward {
        Objects.requireNonNull(recipient);

        if (recipient.isBlank())
            throw new IllegalArgumentException("Recipient cannot be blank.");

        if (amount <= 0)
            throw new IllegalArgumentException("Reward must be positive.");
    }
}
