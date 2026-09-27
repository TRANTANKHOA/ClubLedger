package com.example.domain

import kotlin.math.roundToInt

/**
 * Pure-Kotlin proportional cost allocation engine.
 *
 * Zero Android framework dependencies, so the treasury math can be reused
 * unchanged across JVM, iOS (Kotlin/Native), and Web (Kotlin/Wasm) targets
 * via Kotlin Multiplatform.
 */
object CostAllocationEngine {

    data class MemberAllocation(
        val userId: Long,
        val approvedSessionsCount: Int,
        val percentage: Double,
        val allocatedAmount: Double
    )

    /**
     * Splits [totalAmount] across members in proportion to their share of
     * approved sessions. Each share is rounded to whole cents and the residual
     * rounding difference is assigned to the first member so allocations always
     * sum to exactly [totalAmount].
     *
     * Returns an empty list for an empty [sessionsByUser], and zero-valued
     * allocations when the map has members but no sessions.
     */
    fun allocate(
        totalAmount: Double,
        sessionsByUser: Map<Long, Int>
    ): List<MemberAllocation> {
        if (sessionsByUser.isEmpty()) return emptyList()

        val totalSessions = sessionsByUser.values.sum()
        if (totalSessions == 0) {
            return sessionsByUser.keys.map { MemberAllocation(it, 0, 0.0, 0.0) }
        }

        val allocations = sessionsByUser.map { (userId, sessions) ->
            val share = sessions.toDouble() / totalSessions.toDouble()
            MemberAllocation(
                userId = userId,
                approvedSessionsCount = sessions,
                percentage = (share * 100.0 * 100.0).roundToInt() / 100.0,
                allocatedAmount = (share * totalAmount * 100.0).roundToInt() / 100.0
            )
        }.toMutableList()

        val allocatedSum = allocations.sumOf { it.allocatedAmount }
        val diff = ((totalAmount - allocatedSum) * 100.0).roundToInt() / 100.0
        if (diff != 0.0) {
            val first = allocations[0]
            allocations[0] = first.copy(
                allocatedAmount = ((first.allocatedAmount + diff) * 100.0).roundToInt() / 100.0
            )
        }
        return allocations
    }
}
