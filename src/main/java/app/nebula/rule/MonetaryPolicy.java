package app.nebula.rule;

public final class MonetaryPolicy {

    public static final long INITIAL_SUPPLY = 100_000L;
    public static final float ANNUAL_GROWTH_RATE = 0.018f;
    public static final long EXPECTED_BLOCKS_PER_YEAR = 52_560;

    public long calculateReward() {
        return 1_000L;
    }
}
