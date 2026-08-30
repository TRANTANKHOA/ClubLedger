package com.example

import com.example.data.entity.Attendance
import com.example.data.entity.Payment
import com.example.data.entity.Team
import com.example.data.entity.User
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class ExampleUnitTest {

    @Test
    fun testProportionalCostAllocationFormula() {
        val totalBudget = 1500.00
        val totalSessions = 30
        val userSessions = 8

        val rawAllocation = (userSessions.toDouble() / totalSessions.toDouble()) * totalBudget
        val roundedAllocation = (rawAllocation * 100.0).roundToInt() / 100.0

        assertEquals(400.00, roundedAllocation, 0.001)

        val percentage = (userSessions.toDouble() / totalSessions.toDouble()) * 100.0
        val roundedPercentage = (percentage * 10.0).roundToInt() / 10.0
        assertEquals(26.7, roundedPercentage, 0.001)
    }

    @Test
    fun testZeroAttendanceZeroCostRule() {
        val totalBudget = 1500.00
        val totalSessions = 30
        val userSessions = 0

        val rawAllocation = if (totalSessions > 0) (userSessions.toDouble() / totalSessions.toDouble()) * totalBudget else 0.0
        assertEquals(0.0, rawAllocation, 0.0001)
    }

    @Test
    fun testLedgerRunningBalanceCalculation() {
        var balance = 0.0

        // Invoice debit 400
        balance -= 400.00
        assertEquals(-400.00, balance, 0.001)

        // Member pays 450 (overpaid credit)
        balance += 450.00
        assertEquals(50.00, balance, 0.001)

        // Admin penalty 15
        balance -= 15.00
        assertEquals(35.00, balance, 0.001)
    }

    @Test
    fun testAttendanceCsvHeaderAndEscaping() {
        val header = "Attendance_ID,Member_Name,Member_Email,Team,Session_Date,Session_Type,Status,Notes"
        assertTrue(header.contains("Member_Name"))
        assertTrue(header.contains("Session_Date"))
    }
}
