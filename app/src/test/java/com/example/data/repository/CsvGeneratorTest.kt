package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.ClubDatabase
import com.example.data.entity.Attendance
import com.example.data.entity.BalanceLedger
import com.example.data.entity.Payment
import com.example.data.entity.Team
import com.example.data.entity.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Tests the CSV export generators in [ClubRepository] — headers, quote
 * escaping, column layout, and unknown-entity fallbacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CsvGeneratorTest {

    private lateinit var db: ClubDatabase
    private lateinit var repository: ClubRepository

    private val users = mapOf(
        1L to User(id = 1, name = "Alex Player", email = "alex@club.test"),
        2L to User(id = 2, name = "Marcus, Treasurer", email = "marcus@club.test") // comma in name
    )
    private val teams = mapOf(10L to Team(id = 10, name = "Team Alpha", sportType = "Soccer"))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ClubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ClubRepository(db.clubDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().let {
            it.clear()
            it.set(year, month - 1, day, hour, 0)
            it.timeInMillis
        }

    // --- Attendance CSV ---

    @Test
    fun `attendance csv emits the documented header row`() {
        val csv = repository.generateAttendanceCsv(emptyList(), emptyMap(), emptyMap())
        assertEquals(
            "ID,Date,Member Name,Member Email,Team,Session Type,Status,Notes,Review Notes\n",
            csv
        )
    }

    @Test
    fun `attendance csv maps users and teams and formats dates`() {
        val csv = repository.generateAttendanceCsv(
            listOf(
                Attendance(
                    id = 7, userId = 1, teamId = 10,
                    sessionDate = timestamp(2026, 8, 14),
                    sessionType = "TRAINING", status = "APPROVED",
                    reviewNotes = "Verified on field"
                )
            ),
            users, teams
        )

        val dataRow = csv.trim().lines()[1]
        assertTrue(dataRow.startsWith("7,"))
        assertTrue(dataRow.contains("\"2026-08-14\""))
        assertTrue(dataRow.contains("\"Alex Player\""))
        assertTrue(dataRow.contains("\"alex@club.test\""))
        assertTrue(dataRow.contains("\"Team Alpha\""))
        assertTrue(dataRow.contains("\"TRAINING\""))
        assertTrue(dataRow.contains("\"APPROVED\""))
        assertTrue(dataRow.contains("\"Verified on field\""))
    }

    @Test
    fun `attendance csv escapes embedded quotes by doubling them`() {
        val csv = repository.generateAttendanceCsv(
            listOf(
                Attendance(
                    id = 1, userId = 1, teamId = 10,
                    sessionDate = timestamp(2026, 8, 14),
                    sessionType = "MATCH", status = "PENDING",
                    notes = "Said \"injured\" but played"
                )
            ),
            users, teams
        )
        val dataRow = csv.trim().lines()[1]
        assertTrue("Quotes must be RFC-4180 escaped", dataRow.contains("\"Said \"\"injured\"\" but played\""))
        // Escaped row must still split into exactly 9 columns.
        val columns = dataRow.split(",")
        assertEquals(9, columns.size)
    }

    @Test
    fun `attendance csv falls back to Unknown for missing user or team`() {
        val csv = repository.generateAttendanceCsv(
            listOf(
                Attendance(
                    id = 1, userId = 99, teamId = 99,
                    sessionDate = timestamp(2026, 8, 14),
                    sessionType = "TRAINING", status = "APPROVED"
                )
            ),
            emptyMap(), emptyMap()
        )
        val dataRow = csv.trim().lines()[1]
        assertTrue(dataRow.contains("\"Unknown\""))
        assertFalse(dataRow.contains("null"))
    }

    // --- Payments CSV ---

    @Test
    fun `payments csv emits the documented header and row values`() {
        val csv = repository.generatePaymentsCsv(
            listOf(
                Payment(
                    id = 3, userId = 1, amount = 120.0,
                    paymentDate = timestamp(2026, 8, 10),
                    paymentMethod = "VENMO_ZELLE",
                    referenceNote = "#ZEL-8831",
                    receiptNote = "Screenshot attached",
                    status = "PENDING"
                )
            ),
            users
        )

        val lines = csv.trim().lines()
        assertEquals(
            "ID,Date,Member Name,Amount,Method,Status,Reference Note,Receipt Note,Review Notes",
            lines[0]
        )
        assertTrue(lines[1].startsWith("3,"))
        assertTrue(lines[1].contains("\"2026-08-10\""))
        assertTrue(lines[1].contains("\"Alex Player\""))
        assertTrue(lines[1].contains("120.0,"))
        assertTrue(lines[1].contains("\"VENMO_ZELLE\""))
        assertTrue(lines[1].contains("\"#ZEL-8831\""))
        assertTrue(lines[1].contains("\"Screenshot attached\""))
    }

    @Test
    fun `payments csv handles commas and quotes in member names`() {
        val csv = repository.generatePaymentsCsv(
            listOf(
                Payment(
                    id = 1, userId = 2, amount = 50.0,
                    paymentDate = timestamp(2026, 8, 2), paymentMethod = "CASH"
                )
            ),
            users
        )
        val dataRow = csv.trim().lines()[1]
        assertTrue(dataRow.contains("\"Marcus, Treasurer\""))
    }

    // --- Ledger CSV ---

    @Test
    fun `ledger csv emits the documented header and running balance`() {
        val csv = repository.generateLedgerCsv(
            listOf(
                BalanceLedger(
                    id = 5, userId = 1, type = "INVOICE_DEBIT", amount = -480.0,
                    runningBalanceAfter = -480.0, referenceType = "INVOICE_ALLOCATION",
                    description = "Allocated cost for INV-2026-8-TEA100", createdAt = timestamp(2026, 8, 30, 9)
                )
            ),
            users
        )

        val lines = csv.trim().lines()
        assertEquals(
            "ID,Date,Member Name,Type,Amount,Running Balance,Reference Type,Description",
            lines[0]
        )
        assertTrue(lines[1].startsWith("5,"))
        assertTrue(lines[1].contains("\"2026-08-30 09:00\""))
        assertTrue(lines[1].contains("\"Alex Player\""))
        assertTrue(lines[1].contains("\"INVOICE_DEBIT\""))
        assertTrue(lines[1].contains("-480.0,"))
        assertTrue(lines[1].contains("INV-2026-8-TEA100"))
    }

    @Test
    fun `ledger csv preserves entry order across multiple rows`() {
        val csv = repository.generateLedgerCsv(
            listOf(
                BalanceLedger(id = 1, userId = 1, type = "INVOICE_DEBIT", amount = -480.0,
                    runningBalanceAfter = -480.0, referenceType = "INVOICE_ALLOCATION",
                    description = "Debit", createdAt = timestamp(2026, 8, 1)),
                BalanceLedger(id = 2, userId = 1, type = "PAYMENT_CREDIT", amount = 500.0,
                    runningBalanceAfter = 20.0, referenceType = "PAYMENT",
                    description = "Credit", createdAt = timestamp(2026, 8, 15))
            ),
            users
        )

        val dataLines = csv.trim().lines().drop(1)
        assertEquals(2, dataLines.size)
        assertTrue(dataLines[0].contains("INVOICE_DEBIT"))
        assertTrue(dataLines[1].contains("PAYMENT_CREDIT"))
        assertTrue(dataLines[1].contains("20.0,"))
    }
}
