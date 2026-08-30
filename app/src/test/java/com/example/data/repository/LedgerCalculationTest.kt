package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.ClubDao
import com.example.data.database.ClubDatabase
import com.example.data.entity.Attendance
import com.example.data.entity.Payment
import com.example.data.entity.Team
import com.example.data.entity.TeamBudget
import com.example.data.entity.User
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Tests the running-balance ledger math in [ClubRepository]: payment credits,
 * invoice debits, manual adjustment sign conventions, payment requests, and
 * dispute credits — against a real in-memory Room database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerCalculationTest {

    private lateinit var db: ClubDatabase
    private lateinit var dao: ClubDao
    private lateinit var repository: ClubRepository

    private var adminId = 0L
    private var teamId = 0L
    private var memberId = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ClubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.clubDao()
        repository = ClubRepository(dao)

        adminId = dao.insertUser(User(name = "Marcus Treasury", email = "marcus@club.test", role = "ADMIN"))
        teamId = dao.insertTeam(Team(name = "Team Alpha", sportType = "Soccer"))
        memberId = dao.insertUser(User(name = "Alex Player", email = "alex@club.test"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun dateInPeriod(year: Int, month: Int, day: Int): Long = Calendar.getInstance().let {
        it.set(Calendar.YEAR, year)
        it.set(Calendar.MONTH, month - 1)
        it.set(Calendar.DAY_OF_MONTH, day)
        it.set(Calendar.HOUR_OF_DAY, 12)
        it.clear(Calendar.MINUTE)
        it.clear(Calendar.SECOND)
        it.clear(Calendar.MILLISECOND)
        it.timeInMillis
    }

    @Test
    fun `approving a payment credits the running balance`() = runTest {
        val paymentId = dao.insertPayment(
            Payment(
                userId = memberId,
                amount = 120.00,
                paymentDate = dateInPeriod(2026, 8, 10),
                paymentMethod = "VENMO_ZELLE",
                referenceNote = "#ZEL-8831"
            )
        )

        repository.approvePayment(paymentId, reviewerId = adminId)

        assertEquals("APPROVED", dao.getPaymentById(paymentId)?.status)

        val entry = dao.getLatestLedgerForUser(memberId)
        assertNotNull("Approval must write a ledger entry", entry)
        assertEquals("PAYMENT_CREDIT", entry!!.type)
        assertEquals(120.00, entry.amount, 0.001)
        assertEquals(120.00, entry.runningBalanceAfter, 0.001)
        assertEquals("PAYMENT", entry.referenceType)
        assertTrue(entry.description.contains("#ZEL-8831"))
    }

    @Test
    fun `payment credit stacks on an existing negative balance`() = runTest {
        // Member already owes 480 from an invoice.
        dao.insertLedgerEntry(
            com.example.data.entity.BalanceLedger(
                userId = memberId, type = "INVOICE_DEBIT", amount = -480.00,
                runningBalanceAfter = -480.00, referenceType = "INVOICE_ALLOCATION",
                description = "Seed debit"
            )
        )
        val paymentId = dao.insertPayment(
            Payment(userId = memberId, amount = 500.00,
                paymentDate = dateInPeriod(2026, 8, 12), paymentMethod = "CASH")
        )

        repository.approvePayment(paymentId, reviewerId = adminId)

        val entry = dao.getLatestLedgerForUser(memberId)!!
        // -480 + 500 = +20 overpaid credit.
        assertEquals(20.00, entry.runningBalanceAfter, 0.001)
        assertEquals(500.00, entry.amount, 0.001)
    }

    @Test
    fun `rejecting a payment never touches the ledger`() = runTest {
        val paymentId = dao.insertPayment(
            Payment(userId = memberId, amount = 75.00,
                paymentDate = dateInPeriod(2026, 8, 10), paymentMethod = "CARD")
        )

        repository.rejectPayment(paymentId, reviewerId = adminId, reason = "No bank record found")

        assertEquals("REJECTED", dao.getPaymentById(paymentId)?.status)
        assertNull("Rejected payments must not credit the ledger", dao.getLatestLedgerForUser(memberId))
    }

    @Test
    fun `manual adjustment sign conventions - penalty subtracts and credit adds`() = runTest {
        repository.applyManualBalanceAdjustment(memberId, "PENALTY", 15.00, "Late no-show fine", adminId)
        assertEquals(-15.00, dao.getLatestLedgerForUser(memberId)!!.runningBalanceAfter, 0.001)
        assertEquals(-15.00, dao.getLatestLedgerForUser(memberId)!!.amount, 0.001)

        repository.applyManualBalanceAdjustment(memberId, "CREDIT", 40.00, "Early-bird discount", adminId)
        assertEquals(25.00, dao.getLatestLedgerForUser(memberId)!!.runningBalanceAfter, 0.001)
        assertEquals(40.00, dao.getLatestLedgerForUser(memberId)!!.amount, 0.001)

        repository.applyManualBalanceAdjustment(memberId, "REFUND", 10.00, "Duplicate charge refund", adminId)
        assertEquals(35.00, dao.getLatestLedgerForUser(memberId)!!.runningBalanceAfter, 0.001)

        repository.applyManualBalanceAdjustment(memberId, "CORRECTION", -5.00, "Rounding fix", adminId)
        assertEquals(30.00, dao.getLatestLedgerForUser(memberId)!!.runningBalanceAfter, 0.001)

        // All four adjustments are recorded as immutable ledger history.
        val entries = dao.getAllLedgerEntriesOnce().filter { it.userId == memberId }
        assertEquals(4, entries.size)
        assertTrue(entries.all { it.type == "MANUAL_ADJUSTMENT" })
        assertTrue(entries.any { it.description.contains("[PENALTY] Late no-show fine") })
    }

    @Test
    fun `payment request debits every targeted member once`() = runTest {
        val secondMemberId = dao.insertUser(User(name = "Second Player", email = "second@club.test"))

        val issued = repository.issuePaymentRequest(
            userIds = listOf(memberId, secondMemberId),
            title = "Away tournament registration",
            amount = 35.00,
            memo = "Due before Sept 5",
            adminId = adminId
        )

        assertEquals(2, issued)
        assertEquals(-35.00, dao.getLatestLedgerForUser(memberId)!!.runningBalanceAfter, 0.001)
        assertEquals(-35.00, dao.getLatestLedgerForUser(secondMemberId)!!.runningBalanceAfter, 0.001)
        assertEquals("PAYMENT_REQUEST", dao.getLatestLedgerForUser(memberId)!!.type)
        assertTrue(dao.getLatestLedgerForUser(memberId)!!.description.contains("Away tournament registration"))
    }

    @Test
    fun `dispute resolved with credit adjusts the ledger and status`() = runTest {
        // Seed a debit so the credit has something to offset.
        dao.insertLedgerEntry(
            com.example.data.entity.BalanceLedger(
                userId = memberId, type = "INVOICE_DEBIT", amount = -120.00,
                runningBalanceAfter = -120.00, referenceType = "INVOICE_ALLOCATION",
                description = "Seed debit"
            )
        )
        val disputeId = repository.raiseDispute(
            userId = memberId,
            category = "INVOICE_OVERCHARGE",
            referenceType = "INVOICE_ALLOCATION",
            referenceId = 1L,
            title = "Charged for a session I did not attend",
            description = "I was injured on Aug 14 and did not train.",
            requestedAdjustmentAmount = 60.00
        )

        repository.resolveDispute(
            disputeId = disputeId,
            adminId = adminId,
            resolutionAction = "CREDIT_AND_RESOLVE",
            resolutionNotes = "Attendance record confirmed incorrect",
            creditAmount = 60.00
        )

        val dispute = dao.getDisputeById(disputeId)!!
        assertEquals("RESOLVED_CREDITED", dispute.status)

        val entry = dao.getLatestLedgerForUser(memberId)!!
        assertEquals("REFUND", entry.type)
        assertEquals(60.00, entry.amount, 0.001)
        assertEquals(-60.00, entry.runningBalanceAfter, 0.001)
        assertEquals("DISPUTE_RESOLUTION", entry.referenceType)
    }

    @Test
    fun `dispute resolved by explanation leaves the balance untouched`() = runTest {
        val disputeId = repository.raiseDispute(
            userId = memberId, category = "ATTENDANCE_DISCREPANCY", referenceType = "ATTENDANCE",
            referenceId = 3L, title = "Missing session", description = "My check-in is missing.",
            requestedAdjustmentAmount = 0.0
        )

        repository.resolveDispute(disputeId, adminId, "EXPLAIN_AND_RESOLVE", "Session was self-reported without proof", 0.0)

        assertEquals("RESOLVED_EXPLAINED", dao.getDisputeById(disputeId)?.status)
        assertNull("Explanation-only resolution must not credit the ledger", dao.getLatestLedgerForUser(memberId))
    }

    @Test
    fun `end to end ledger sequence - debit then payment then adjustment`() = runTest {
        // 1. Invoice allocation debit of 480 (8 of 10 sessions of a $600 budget).
        repeat(8) { index ->
            dao.insertAttendance(
                Attendance(userId = memberId, teamId = teamId,
                    sessionDate = dateInPeriod(2026, 8, 3 + index),
                    sessionType = "TRAINING", status = "APPROVED")
            )
        }
        repeat(2) { index ->
            dao.insertAttendance(
                Attendance(userId = adminId, teamId = teamId,
                    sessionDate = dateInPeriod(2026, 8, 4 + index),
                    sessionType = "TRAINING", status = "APPROVED")
            )
        }
        val budgetId = repository.createBudget(
            TeamBudget(teamId = teamId, periodMonth = 8, periodYear = 2026,
                totalAmount = 600.00, title = "August Pitch Rental"),
            adminId = adminId
        )
        repository.generateInvoiceAndAllocate(budgetId, adminId).getOrThrow()
        assertEquals(-480.00, dao.getLatestLedgerForUser(memberId)!!.runningBalanceAfter, 0.001)

        // 2. Member pays 500 — lands on +20 credit.
        val paymentId = dao.insertPayment(
            Payment(userId = memberId, amount = 500.00,
                paymentDate = dateInPeriod(2026, 8, 28), paymentMethod = "BANK_TRANSFER",
                referenceNote = "#TRF-2210")
        )
        repository.approvePayment(paymentId, adminId)
        assertEquals(20.00, dao.getLatestLedgerForUser(memberId)!!.runningBalanceAfter, 0.001)

        // 3. Admin applies a 25 penalty for a no-show fine — final balance -5.
        repository.applyManualBalanceAdjustment(memberId, "PENALTY", 25.00, "Missed final fixture", adminId)
        assertEquals(-5.00, dao.getLatestLedgerForUser(memberId)!!.runningBalanceAfter, 0.001)

        // Full history is preserved in chronological order: DEBIT, CREDIT, ADJUSTMENT.
        val history = dao.getAllLedgerEntriesOnce()
            .filter { it.userId == memberId }
            .sortedBy { it.id }
        assertEquals(listOf("INVOICE_DEBIT", "PAYMENT_CREDIT", "MANUAL_ADJUSTMENT"), history.map { it.type })
    }
}
