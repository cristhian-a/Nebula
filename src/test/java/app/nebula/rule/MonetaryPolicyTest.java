
package app.nebula.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MonetaryPolicyTest {

    @Test
    void calculateRewardReturnsBootstrapRewardForZeroSupply() {
        long reward = MonetaryPolicy.calculateReward(0L);

        assertEquals(MonetaryPolicy.BOOTSTRAP_REWARD_CENTS, reward);
    }

    @Test
    void calculateRewardRejectsNegativeSupply() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MonetaryPolicy.calculateReward(-1L)
        );
    }

    @Test
    void calculateRewardReturnsBootstrapRewardBelowTransitionSupply() {
        long supply = MonetaryPolicy.TRANSITION_SUPPLY_CENTS - 1L;

        long reward = MonetaryPolicy.calculateReward(supply);

        assertEquals(MonetaryPolicy.BOOTSTRAP_REWARD_CENTS, reward);
    }

    @Test
    void calculateRewardReturnsBootstrapRewardAtTransitionSupply() {
        long reward = MonetaryPolicy.calculateReward(
                MonetaryPolicy.TRANSITION_SUPPLY_CENTS
        );

        assertEquals(MonetaryPolicy.BOOTSTRAP_REWARD_CENTS, reward);
    }

    @Test
    void calculateRewardUsesPercentageIssuanceAboveTransitionSupply() {
        // This increase in supply makes the calculated reward exactly
        // one cent higher than the bootstrap reward.
        long supply = MonetaryPolicy.TRANSITION_SUPPLY_CENTS + 5_256_000L;

        long reward = MonetaryPolicy.calculateReward(supply);

        assertEquals(
                MonetaryPolicy.BOOTSTRAP_REWARD_CENTS + 1L,
                reward
        );
    }
}
