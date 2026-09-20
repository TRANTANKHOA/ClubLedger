package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.ClubDao
import com.example.data.database.ClubDatabase
import com.example.data.entity.Attendance
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
 * Tests the proportional cost allocation engine against a real in-memory
 * Room database, mirroring the documented "Proportional Fair-Share Facility
 * Allocation" use case (README Use Case 1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CostAllocationEngineTest {

    private lateinit var db: ClubDatabase
    private lateinit var dao: ClubDao
    private lateinit var repository: ClubRepository

    private var teamId = 0L
    private var adminId = 0L
    private var playerAId = 0L
    private var playerBId = 0L
    private var playerCId = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ClubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.clubDao()
        repository = ClubRepository(dao)

        adminId = dao.insertUser(User(name = "Marcus Treasury", email = "marcus@club.test", role = "ADMIN"))
        teamId = dao.insertTeam(
            Team(name = "Team Alpha", sportType = "Soccer", inviteCode = "ALPHA-26")
        )
        playerAId = dao.insertUser(User(name = "Player A", email = "a@club.test"))
        playerBId = dao.insertUser(User(name = "Player B", email = "b@club.test"))
        playerCId = dao.insertUser(User(name = "Player C", email = "c@club.test"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Timestamp safely inside the given billing month (mirrors the repository's period window). */
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

    private suspend fun seedApprovedAttendance(userId: Long, count: Int, year: Int, month: Int) {
        repeat(count) { index ->
            dao.insertAttendance(
                Attendance(
                    userId = userId,
                    teamId = teamId,
                    sessionDate = dateInPeriod(year, month, 5 + index),
                    sessionType = "TRAINING",
                    status = "APPROVED"
                )
            )
        }
    }

    private suspend fun createAugustBudget(total: Double): Long =
        repository.createBudget(
            TeamBudget(
                teamId = teamId,
                periodMonth = 8,
                periodYear = 2026,
                totalAmount = total,
                title = "August Pitch Rental"
            ),
            adminId = adminId
        )

    @Test
    fun `readme use case 1 - 600 dollars split 8-2-0 sessions allocates 480-120-0`() = runTest {
        // Player A attended 8 sessions, B attended 2, C attended 0 (injured).
        seedApprovedAttendance(playerAId, 8, 2026, 8)
        seedApprovedAttendance(playerBId, 2, 2026, 8)
        // Player C has zero approved attendance — no rows inserted.
        val budgetId = createAugustBudget(600.00)

        val result = repository.generateInvoiceAndAllocate(budgetId, adminId)

        assertTrue("Allocation should succeed", result.isSuccess)
        val invoice = result.getOrThrow()
        assertEquals(10, invoice.totalApprovedSessions)
        assertEquals(60.00, invoice.costPerSession, 0.001) // 600 / 10 sessions

        val allocations = dao.getAllocationsForInvoiceOnce(invoice.id)
        assertEquals("Only attending members receive an allocation", 2, allocations.size)

        val allocA = allocations.first { it.userId == playerAId }
        val allocB = allocations.first { it.userId == playerBId }

        assertEquals(8, allocA.approvedSessionsCount)
        assertEquals(80.0, allocA.percentage, 0.001)
        assertEquals(480.00, allocA.allocatedAmount, 0.001)

        assertEquals(2, allocB.approvedSessionsCount)
        assertEquals(20.0, allocB.percentage, 0.001)
        assertEquals(120.00, allocB.allocatedAmount, 0.001)

        // Zero-attendance guarantee: Player C is billed nothing and never appears.
        assertNull("Zero-attendance member must not be invoiced", allocations.firstOrNull { it.userId == playerCId })
        assertEquals(0.00, allocations.filter { it.userId == playerCId }.sumOf { it.allocatedAmount }, 0.001)

        // Budget flips to INVOICED once allocated.
        assertEquals("INVOICED", dao.getBudgetById(budgetId)?.status)
    }

    @Test
    fun `allocations always sum exactly to the budget despite cent rounding`() = runTest {
        // $100 split as 3/2/2 sessions (7 total) produces non-terminating shares.
        seedApprovedAttendance(playerAId, 3, 2026, 8)
        seedApprovedAttendance(playerBId, 2, 2026, 8)
        seedApprovedAttendance(playerCId, 2, 2026, 8)
        val budgetId = createAugustBudget(100.00)

        val invoice = repository.generateInvoiceAndAllocate(budgetId, adminId).getOrThrow()
        val allocations = dao.getAllocationsForInvoiceOnce(invoice.id)

        assertEquals(3, allocations.size)
        assertEquals(
            "Rounded allocations must conserve the budget to the cent",
            100.00,
            allocations.sumOf { it.allocatedAmount },
            0.0001
        )
        assertEquals(42.86, allocations.first { it.userId == playerAId }.allocatedAmount, 0.001)
    }

    @Test
    fun `invoice debit is posted to each member ledger with negative amount`() = runTest {
        seedApprovedAttendance(playerAId, 8, 2026, 8)
        seedApprovedAttendance(playerBId, 2, 2026, 8)
        val budgetId = createAugustBudget(600.00)

        val invoice = repository.generateInvoiceAndAllocate(budgetId, adminId).getOrThrow()

        val ledgerA = dao.getLatestLedgerForUser(playerAId)
        assertNotNull("Invoice debit must hit the ledger", ledgerA)
        assertEquals("INVOICE_DEBIT", ledgerA!!.type)
        assertEquals(-480.00, ledgerA.amount, 0.001)
        assertEquals(-480.00, ledgerA.runningBalanceAfter, 0.001)
        assertEquals("INVOICE_ALLOCATION", ledgerA.referenceType)
        assertTrue(ledgerA.description.contains(invoice.invoiceNumber))
    }

    @Test
    fun `pending and rejected attendance never counts towards allocation`() = runTest {
        seedApprovedAttendance(playerAId, 2, 2026, 8)
        dao.insertAttendance(
            Attendance(userId = playerAId, teamId = teamId, sessionDate = dateInPeriod(2026, 8, 20),
                sessionType = "MATCH", status = "PENDING")
        )
        dao.insertAttendance(
            Attendance(userId = playerBId, teamId = teamId, sessionDate = dateInPeriod(2026, 8, 21),
                sessionType = "MATCH", status = "REJECTED")
        )
        val budgetId = createAugustBudget(300.00)

        val invoice = repository.generateInvoiceAndAllocate(budgetId, adminId).getOrThrow()
        val allocations = dao.getAllocationsForInvoiceOnce(invoice.id)

        assertEquals("Only Player A's 2 approved sessions count", 1, allocations.size)
        assertEquals(2, invoice.totalApprovedSessions)
        assertEquals(300.00, allocations.single().allocatedAmount, 0.001)
    }

    @Test
    fun `attendance from other months is excluded from the billing period`() = runTest {
        seedApprovedAttendance(playerAId, 5, 2026, 7) // July sessions
        seedApprovedAttendance(playerBId, 4, 2026, 8) // August sessions
        val budgetId = createAugustBudget(400.00)

        val invoice = repository.generateInvoiceAndAllocate(budgetId, adminId).getOrThrow()
        val allocations = dao.getAllocationsForInvoiceOnce(invoice.id)

        assertEquals(4, invoice.totalApprovedSessions)
        assertEquals("July attendee must not be billed for August", 400.00,
            allocations.first { it.userId == playerBId }.allocatedAmount, 0.001)
        assertNull(allocations.firstOrNull { it.userId == playerAId })
    }

    @Test
    fun `allocation fails with clear error when no attendance is approved yet`() = runTest {
        val budgetId = createAugustBudget(500.00)

        val result = repository.generateInvoiceAndAllocate(budgetId, adminId)

        assertTrue(result.isFailure)
        assertTrue(
            "Error must guide the admin to approve attendance first",
            result.exceptionOrNull()?.message.orEmpty().contains("approve", ignoreCase = true)
        )
        assertEquals("Budget must remain un-invoiced", "APPROVED", dao.getBudgetById(budgetId)?.status)
    }

    @Test
    fun `allocation fails when budget does not exist`() = runTest {
        val result = repository.generateInvoiceAndAllocate(budgetId = 9999L, adminId = adminId)
        assertTrue(result.isFailure)
    }

    @Test
    fun `recalculate after late attendance approval rebalances without double billing`() = runTest {
        seedApprovedAttendance(playerAId, 8, 2026, 8)
        seedApprovedAttendance(playerBId, 2, 2026, 8)
        val budgetId = createAugustBudget(600.00)

        val invoice = repository.generateInvoiceAndAllocate(budgetId, adminId).getOrThrow()
        assertEquals(-480.00, dao.getLatestLedgerForUser(playerAId)!!.runningBalanceAfter, 0.001)

        // Two more of B's sessions get approved after invoicing.
        seedApprovedAttendance(playerBId, 2, 2026, 8)

        val recalculated = repository.recalculateInvoice(invoice.id, adminId)

        assertTrue(recalculated.isSuccess)
        val updated = recalculated.getOrThrow()
        assertEquals(12, updated.totalApprovedSessions)

        val allocations = dao.getAllocationsForInvoiceOnce(invoice.id)
        assertEquals("Rebalancing must keep one allocation per member", 2, allocations.size)
        assertEquals(400.00, allocations.first { it.userId == playerAId }.allocatedAmount, 0.001)
        assertEquals(200.00, allocations.first { it.userId == playerBId }.allocatedAmount, 0.001)

        // Exactly one live INVOICE_DEBIT per member — no duplicated charges.
        val debitsA = dao.getAllLedgerEntriesOnce()
            .filter { it.userId == playerAId && it.type == "INVOICE_DEBIT" }
        assertEquals("Recalculation must replace, not stack, debits", 1, debitsA.size)
        assertEquals(-400.00, dao.getLatestLedgerForUser(playerAId)!!.runningBalanceAfter, 0.001)
    }
}
