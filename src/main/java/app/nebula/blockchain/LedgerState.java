package app.nebula.blockchain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LedgerState {

    private final Map<String, Long> balances = new HashMap<>();

    public LedgerState(Map<String, Long> initialBalances) {
        if (initialBalances != null) {
            initialBalances.forEach((address, balance) -> {
                Objects.requireNonNull(address);
                Objects.requireNonNull(balance);

                if (address.isBlank())
                    throw new IllegalArgumentException("Addresses cannot be null.");
                if (balance < 0)
                    throw new IllegalArgumentException("Balance cannot be negative.");
                if (balance > 0)
                    balances.put(address, balance);
            });
        }
    }

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
}
