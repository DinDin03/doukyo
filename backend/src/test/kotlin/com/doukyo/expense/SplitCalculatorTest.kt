package com.doukyo.expense

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import kotlin.random.Random

// Pure unit tests — no Spring, no database. That is the whole point of keeping the
// split maths free of I/O: it can be checked exhaustively instead of by example.
class SplitCalculatorTest {

    // ---------------------------------------------------------------
    // LAWS — these must hold for EVERY input, not just the ones below.
    // Checked over thousands of generated cases with a fixed seed, so a
    // failure is always reproducible.
    // ---------------------------------------------------------------

    @Test
    fun `law - shares always sum exactly to the total`() {
        val random = Random(seed = 20260907)
        repeat(5_000) {
            val total = random.nextLong(1, 5_000_00)
            val weights = List(random.nextInt(1, 9)) { random.nextLong(1, 100) }

            val shares = SplitCalculator.byWeights(total, weights)

            assertThat(shares.sum())
                .withFailMessage("total=%d weights=%s produced %s", total, weights, shares)
                .isEqualTo(total)
        }
    }

    @Test
    fun `law - no share is ever negative`() {
        val random = Random(seed = 11)
        repeat(2_000) {
            val total = random.nextLong(1, 1_000_00)
            val weights = List(random.nextInt(1, 12)) { random.nextLong(1, 500) }

            assertThat(SplitCalculator.byWeights(total, weights)).allMatch { it >= 0 }
        }
    }

    @Test
    fun `law - nobody is more than one cent from their exact share`() {
        val random = Random(seed = 7)
        repeat(2_000) {
            val total = random.nextLong(1, 1_000_00)
            val weights = List(random.nextInt(1, 10)) { random.nextLong(1, 50) }
            val weightSum = weights.sum()

            val shares = SplitCalculator.byWeights(total, weights)

            // Exact share is the rational total*weight/weightSum. Compare without
            // floating point: |share*weightSum - total*weight| < weightSum.
            shares.forEachIndexed { i, share ->
                val drift = Math.abs(share * weightSum - total * weights[i])
                assertThat(drift)
                    .withFailMessage("total=%d weights=%s share[%d]=%d", total, weights, i, share)
                    .isLessThan(weightSum)
            }
        }
    }

    @Test
    fun `law - the same input always produces the same output`() {
        // Determinism is a correctness property: editing an expense recomputes the
        // split, and the leftover cent must not wander between people.
        val random = Random(seed = 99)
        repeat(500) {
            val total = random.nextLong(1, 100_00)
            val weights = List(random.nextInt(2, 7)) { random.nextLong(1, 20) }

            assertThat(SplitCalculator.byWeights(total, weights))
                .isEqualTo(SplitCalculator.byWeights(total, weights))
        }
    }

    @Test
    fun `law - the leftover goes to the largest remainders`() {
        // 10000 split 1:2 — the second party is cut by .67 of a cent, the first by
        // .33, so the spare cent must go to the second.
        assertThat(SplitCalculator.byWeights(10_000, listOf(1, 2))).containsExactly(3_333, 6_667)
    }

    // ---------------------------------------------------------------
    // EVEN SPLITS
    // ---------------------------------------------------------------

    @Test
    fun `an even split that divides cleanly`() {
        assertThat(SplitCalculator.evenly(9_000, 3)).containsExactly(3_000, 3_000, 3_000)
    }

    @Test
    fun `100 dollars three ways loses no cent`() {
        // The canonical case: 10000 / 3 = 3333.33, and 3333*3 is only 9999.
        assertThat(SplitCalculator.evenly(10_000, 3)).containsExactly(3_334, 3_333, 3_333)
    }

    @Test
    fun `two leftover cents go to the first two people`() {
        assertThat(SplitCalculator.evenly(10_001, 3)).containsExactly(3_334, 3_334, 3_333)
    }

    @Test
    fun `a single participant owes the whole amount`() {
        assertThat(SplitCalculator.evenly(8_420, 1)).containsExactly(8_420)
    }

    @Test
    fun `one cent between three people is not lost`() {
        assertThat(SplitCalculator.evenly(1, 3)).containsExactly(1, 0, 0)
    }

    @Test
    fun `an even split survives a very large amount`() {
        val shares = SplitCalculator.evenly(999_999_999_999, 7)
        assertThat(shares.sum()).isEqualTo(999_999_999_999)
    }

    // ---------------------------------------------------------------
    // WEIGHTED SPLITS
    // ---------------------------------------------------------------

    @Test
    fun `weights divide proportionally`() {
        // Ravi has the big room and pays double.
        assertThat(SplitCalculator.byWeights(10_000, listOf(2, 1, 1)))
            .containsExactly(5_000, 2_500, 2_500)
    }

    @Test
    fun `equal weights behave exactly like an even split`() {
        assertThat(SplitCalculator.byWeights(10_000, listOf(1, 1, 1)))
            .isEqualTo(SplitCalculator.evenly(10_000, 3))
    }

    @Test
    fun `weights are scale invariant`() {
        // 2:1:1 and 20:10:10 describe the same division.
        assertThat(SplitCalculator.byWeights(7_777, listOf(2, 1, 1)))
            .isEqualTo(SplitCalculator.byWeights(7_777, listOf(20, 10, 10)))
    }

    // ---------------------------------------------------------------
    // PERCENTAGE (BASIS POINTS) — 10000 bp = 100%
    // ---------------------------------------------------------------

    @Test
    fun `basis points split by percentage`() {
        // 50% / 25% / 25%
        assertThat(SplitCalculator.byBasisPoints(10_000, listOf(5_000, 2_500, 2_500)))
            .containsExactly(5_000, 2_500, 2_500)
    }

    @Test
    fun `basis points express thirds, which whole percents cannot`() {
        // 33.33 / 33.33 / 33.34 — the reason we chose basis points over whole percent.
        val shares = SplitCalculator.byBasisPoints(10_000, listOf(3_333, 3_333, 3_334))
        assertThat(shares.sum()).isEqualTo(10_000)
        assertThat(shares).containsExactly(3_333, 3_333, 3_334)
    }

    @Test
    fun `basis points must add up to exactly one hundred percent`() {
        assertThatThrownBy { SplitCalculator.byBasisPoints(10_000, listOf(5_000, 2_500)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("100%")

        assertThatThrownBy { SplitCalculator.byBasisPoints(10_000, listOf(5_000, 5_000, 1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("100%")
    }

    @Test
    fun `a single participant at one hundred percent takes everything`() {
        assertThat(SplitCalculator.byBasisPoints(4_242, listOf(10_000))).containsExactly(4_242)
    }

    // ---------------------------------------------------------------
    // EXACT AMOUNTS — a validator, not a distributor
    // ---------------------------------------------------------------

    @Test
    fun `exact amounts pass through unchanged when they sum to the total`() {
        assertThat(SplitCalculator.exact(10_000, listOf(6_000, 3_000, 1_000)))
            .containsExactly(6_000, 3_000, 1_000)
    }

    @Test
    fun `exact amounts allow a participant who owes nothing`() {
        assertThat(SplitCalculator.exact(10_000, listOf(10_000, 0))).containsExactly(10_000, 0)
    }

    @Test
    fun `exact amounts that do not sum to the total are rejected`() {
        assertThatThrownBy { SplitCalculator.exact(10_000, listOf(6_000, 3_000)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("must add up")

        assertThatThrownBy { SplitCalculator.exact(10_000, listOf(6_000, 5_000)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `exact amounts cannot be negative`() {
        assertThatThrownBy { SplitCalculator.exact(10_000, listOf(11_000, -1_000)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("negative")
    }

    // ---------------------------------------------------------------
    // VALIDATION
    // ---------------------------------------------------------------

    @Test
    fun `a non-positive total is rejected`() {
        listOf(0L, -1L).forEach { bad ->
            assertThatThrownBy { SplitCalculator.evenly(bad, 3) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("greater than zero")
        }
    }

    @Test
    fun `an empty participant list is rejected`() {
        assertThatThrownBy { SplitCalculator.byWeights(10_000, emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("at least one")

        assertThatThrownBy { SplitCalculator.evenly(10_000, 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `non-positive weights are rejected`() {
        // A zero weight is not "owes nothing" — it is a participant who should not
        // have been included. Saying so loudly beats silently assigning 0.
        assertThatThrownBy { SplitCalculator.byWeights(10_000, listOf(1, 0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("greater than zero")

        assertThatThrownBy { SplitCalculator.byWeights(10_000, listOf(2, -1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
