package com.doukyo.expense

// Divides an amount of money into exact cent shares. Pure: no I/O, no state, no
// clock — the same input always gives the same output, which is what lets an edit
// recompute a split without the leftover cent wandering between people.
//
// Everything stays in integers, including the remainder ranking. Introducing a
// float to rank remainders would reintroduce exactly the imprecision that storing
// money in cents exists to avoid.
object SplitCalculator {

    const val BASIS_POINTS_TOTAL = 10_000 // 10000bp = 100%; 1bp = 0.01%

    // Equal weights. "Split it three ways."
    fun evenly(totalCents: Long, participants: Int): List<Long> {
        require(participants >= 1) { "A split needs at least one participant" }
        return byWeights(totalCents, List(participants) { 1L })
    }

    // Percentages, expressed in basis points so thirds are representable —
    // 33.33% is 3333, which whole percents cannot express.
    fun byBasisPoints(totalCents: Long, basisPoints: List<Int>): List<Long> {
        require(basisPoints.sum() == BASIS_POINTS_TOTAL) {
            "Percentages must add up to 100% (10000 basis points)"
        }
        return byWeights(totalCents, basisPoints.map { it.toLong() })
    }

    // Caller-supplied amounts: nothing is divided, so this only validates.
    fun exact(totalCents: Long, amounts: List<Long>): List<Long> {
        requirePositiveTotal(totalCents)
        require(amounts.isNotEmpty()) { "A split needs at least one participant" }
        require(amounts.none { it < 0 }) { "A share cannot be negative" }
        require(amounts.sum() == totalCents) { "Shares must add up to the total" }
        return amounts
    }

    // The one algorithm the other modes are built from.
    //
    // Largest remainder method: floor every share, then hand the leftover cents to
    // whoever was cut by the most. Guarantees the shares sum to the total by
    // construction, and that nobody is more than one cent from their true share.
    fun byWeights(totalCents: Long, weights: List<Long>): List<Long> {
        requirePositiveTotal(totalCents)
        require(weights.isNotEmpty()) { "A split needs at least one participant" }
        require(weights.all { it > 0 }) { "Every weight must be greater than zero" }

        val weightSum = weights.sum()
        val shares = LongArray(weights.size)
        val remainders = LongArray(weights.size)

        weights.forEachIndexed { i, weight ->
            val numerator = totalCents * weight
            shares[i] = numerator / weightSum   // floor
            remainders[i] = numerator % weightSum // the discarded fraction, as an integer
        }

        // Whatever the flooring dropped, given out one cent at a time. Ties break on
        // index so the result is stable rather than dependent on sort internals.
        val leftover = totalCents - shares.sum()
        weights.indices
            .sortedWith(compareByDescending<Int> { remainders[it] }.thenBy { it })
            .take(leftover.toInt())
            .forEach { shares[it]++ }

        return shares.toList()
    }

    private fun requirePositiveTotal(totalCents: Long) =
        require(totalCents > 0) { "An amount must be greater than zero" }
}
