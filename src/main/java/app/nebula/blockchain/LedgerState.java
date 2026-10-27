package app.nebula.blockchain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LedgerState {

    private final Map<String, Long> balances = new HashMap<>();

    public long getBalance(String address) {
        Objects.requireNonNull(address);
        return balances.getOrDefault(address, 0L);
    }

    public void apply(Transaction transaction) {
        Objects.requireNonNull(transaction);

        if (!transaction.verifySignature())
            throw new IllegalArgumentException("Invalid transaction.");

        String sender = transaction.sender();
        String recipient = transaction.recipient();
        long amount = transaction.amount();

        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0.");

        if (sender.equals(recipient))
            return;

        long senderBalance = getBalance(sender);
        long recipientBalance = getBalance(recipient);

        if (senderBalance < amount)
            throw new IllegalArgumentException("No sufficient funds to apply the transaction.");

        senderBalance -= amount;
        recipientBalance = Math.addExact(amount, recipientBalance);

        if (senderBalance == 0)
            balances.remove(sender);
        else
            balances.put(sender, senderBalance);

        balances.put(recipient, recipientBalance);
    }

    public void applyReward(MiningReward reward) {
        Objects.requireNonNull(reward);

        long currentBalance = getBalance(reward.recipient());
        long newBalance = Math.addExact(currentBalance, reward.amount());

        balances.put(reward.recipient(), newBalance);
    }

    public long getTotalSupplyCents() {
        long totalSupply = 0;

        for (long v : balances.values()) {
            totalSupply = Math.addExact(totalSupply, v);
        }

        return totalSupply;
    }
}
