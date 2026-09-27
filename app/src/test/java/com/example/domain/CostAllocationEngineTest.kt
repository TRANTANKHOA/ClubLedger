package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the platform-neutral allocation engine. These run
 * without Robolectric because the engine has zero Android dependencies —
 * the same guarantee that makes it reusable from Kotlin Multiplatform
 * iOS/JVM/Web targets.
 */
class CostAllocationEngineTest {

    @Test
    fun `penny remainder is assigned to the first member`() {
        // $100.00 over 1/1/1 sessions: 33.33 each rounds the pot down a cent.
        val result = CostAllocationEngine.allocate(
            totalAmount = 100.00,
            sessionsByUser = mapOf(1L to 1, 2L to 1, 3L to 1)
        )

        assertEquals(3, result.size)
        assertEquals(33.34, result[0].allocatedAmount, 0.0001)
        assertEquals(33.33, result[1].allocatedAmount, 0.0001)
        assertEquals(33.33, result[2].allocatedAmount, 0.0001)
        assertEquals(100.00, result.sumOf { it.allocatedAmount }, 0.0001)
    }

    @Test
    fun `rounding overshoot is clawed back from the first member`() {
        // $0.05 over 3/3 sessions: 0.025 each rounds UP to 0.03, so the pot
        // overshoots by a cent — the first member absorbs the negative residual.
        val result = CostAllocationEngine.allocate(
            totalAmount = 0.05,
            sessionsByUser = mapOf(1L to 3, 2L to 3)
        )

        assertEquals(0.02, result[0].allocatedAmount, 0.0001)
        assertEquals(0.03, result[1].allocatedAmount, 0.0001)
        assertEquals("Negative residual must still conserve the total", 0.05, result.sumOf { it.allocatedAmount }, 0.0001)
    }

    @Test
    fun `zero total amount allocates zeros but still reports session shares`() {
        val result = CostAllocationEngine.allocate(
            totalAmount = 0.00,
            sessionsByUser = mapOf(1L to 2, 2L to 3)
        )

        assertEquals(2, result.size)
        val byUser = result.associateBy { it.userId }
        assertEquals(0.0, byUser.getValue(1L).allocatedAmount, 0.0)
        assertEquals(0.0, byUser.getValue(2L).allocatedAmount, 0.0)
        // Percentages track session shares regardless of the pot being empty.
        assertEquals(40.0, byUser.getValue(1L).percentage, 0.0001)
        assertEquals(60.0, byUser.getValue(2L).percentage, 0.0001)
    }

    @Test
    fun `exact split needs no residual adjustment`() {
        val result = CostAllocationEngine.allocate(
            totalAmount = 100.00,
            sessionsByUser = mapOf(1L to 3, 2L to 1)
        )

        assertEquals(75.00, result[0].allocatedAmount, 0.0001)
        assertEquals(25.00, result[1].allocatedAmount, 0.0001)
        assertEquals(75.0, result[0].percentage, 0.0001)
        assertEquals(25.0, result[1].percentage, 0.0001)
    }

    @Test
    fun `members with zero total sessions receive zero allocations`() {
        val result = CostAllocationEngine.allocate(
            totalAmount = 500.00,
            sessionsByUser = mapOf(1L to 0, 2L to 0)
        )

        assertEquals(2, result.size)
        result.forEach {
            assertEquals(0, it.approvedSessionsCount)
            assertEquals(0.0, it.percentage, 0.0)
            assertEquals(0.0, it.allocatedAmount, 0.0)
        }
    }

    @Test
    fun `empty session map yields empty allocation list`() {
        val result = CostAllocationEngine.allocate(totalAmount = 500.00, sessionsByUser = emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single member absorbs the full amount`() {
        val result = CostAllocationEngine.allocate(
            totalAmount = 250.00,
            sessionsByUser = mapOf(42L to 4)
        )

        val only = result.single()
        assertEquals(42L, only.userId)
        assertEquals(4, only.approvedSessionsCount)
        assertEquals(100.0, only.percentage, 0.0001)
        assertEquals(250.00, only.allocatedAmount, 0.0001)
    }

    @Test
    fun `allocations always conserve the total across uneven splits`() {
        val cases = listOf(
            100.00 to mapOf(1L to 1, 2L to 1, 3L to 1),
            50.55 to mapOf(10L to 2, 20L to 3),
            1000.00 to mapOf(1L to 7, 2L to 7, 3L to 7, 4L to 2),
            0.01 to mapOf(1L to 2, 2L to 3, 3L to 5)
        )
        for ((total, sessions) in cases) {
            val result = CostAllocationEngine.allocate(total, sessions)
            assertEquals(
                "sum of allocated amounts for $sessions",
                total,
                result.sumOf { it.allocatedAmount },
                0.0001
            )
        }
    }

    @Test
    fun `session counts and percentages are reported per member`() {
        val result = CostAllocationEngine.allocate(
            totalAmount = 600.00,
            sessionsByUser = mapOf(7L to 8, 8L to 2)
        )

        val byUser = result.associateBy { it.userId }
        assertEquals(8, byUser.getValue(7L).approvedSessionsCount)
        assertEquals(2, byUser.getValue(8L).approvedSessionsCount)
        assertEquals(80.0, byUser.getValue(7L).percentage, 0.0001)
        assertEquals(20.0, byUser.getValue(8L).percentage, 0.0001)
        assertEquals(480.00, byUser.getValue(7L).allocatedAmount, 0.0001)
        assertEquals(120.00, byUser.getValue(8L).allocatedAmount, 0.0001)
    }
}
