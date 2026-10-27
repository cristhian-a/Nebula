package app.nebula.rule;

import java.math.BigInteger;

public final class MonetaryPolicy {

    public static final long BOOTSTRAP_REWARD_CENTS = 15_000L;
    // 100 = 1%
    public static final int ANNUAL_GROWTH_RATE_BASIS_POINTS = 100;
    // 365 days * 24 hours * 6 blocks per hour.
    public static final long EXPECTED_BLOCKS_PER_YEAR = 52_560;

    private static final long BASIS_POINTS_PER_WHOLE = 10_000L;

    public static final long TRANSITION_SUPPLY_CENTS = BigInteger
            .valueOf(BOOTSTRAP_REWARD_CENTS)
            .multiply(BigInteger.valueOf(EXPECTED_BLOCKS_PER_YEAR))
            .multiply(BigInteger.valueOf(BASIS_POINTS_PER_WHOLE))
            .divide(BigInteger.valueOf(ANNUAL_GROWTH_RATE_BASIS_POINTS))
            .longValueExact();

    private MonetaryPolicy() {}

    public static long calculateReward(long currentlySupplyCents) {
        if (currentlySupplyCents < 0)
            throw new IllegalArgumentException("Current supply cannot be negative.");

        if (currentlySupplyCents < TRANSITION_SUPPLY_CENTS)
            return BOOTSTRAP_REWARD_CENTS;

        return BigInteger.valueOf(currentlySupplyCents)
                .multiply(BigInteger.valueOf(ANNUAL_GROWTH_RATE_BASIS_POINTS))
                .divide(BigInteger.valueOf(BASIS_POINTS_PER_WHOLE))
                .divide(BigInteger.valueOf(EXPECTED_BLOCKS_PER_YEAR))
                .longValueExact();
    }
}
