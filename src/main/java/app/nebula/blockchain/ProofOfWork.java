package app.nebula.blockchain;

public final class ProofOfWork {

    private ProofOfWork() {}

    public static Block mine(Block block, int difficulty) {
        if (difficulty < 0 || difficulty > 64) {
            throw new IllegalArgumentException("Difficulty must be between 0 and 64");
        }

        String target = "0".repeat(difficulty);
        long nonce = 0;

        while (true) {
            Block candidate = new Block(
                    block.index(),
                    block.timestamp(),
                    block.data(),
                    block.previousHash(),
                    nonce,
                    null
            );

            if (candidate.hash().startsWith(target))
                return candidate;

            if (nonce == Long.MAX_VALUE)
                throw new IllegalStateException("Nonce space exhausted");

            nonce++;
        }
    }

    public static boolean isValid(Block block, int difficulty) {
        if (difficulty < 0 || difficulty > 64)
            return false;

        return block.hash().equals(block.calculateHash()) &&
                block.hash().startsWith("0".repeat(difficulty));
    }
}
