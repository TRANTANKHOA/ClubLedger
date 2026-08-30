package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {

    // --- Users ---
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY name ASC")
    suspend fun getAllUsersOnce(): List<User>

    @Query("SELECT * FROM users WHERE role = 'MEMBER' ORDER BY name ASC")
    fun getMembers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserByIdOnce(userId: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    // --- Teams ---
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllTeams(): Flow<List<Team>>

    @Query("SELECT * FROM teams ORDER BY name ASC")
    suspend fun getAllTeamsOnce(): List<Team>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun getTeamById(teamId: Long): Flow<Team?>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamByIdOnce(teamId: Long): Team?

    @Query("SELECT * FROM teams WHERE LOWER(inviteCode) = LOWER(:inviteCode) LIMIT 1")
    fun getTeamByInviteCode(inviteCode: String): Flow<Team?>

    @Query("SELECT * FROM teams WHERE LOWER(inviteCode) = LOWER(:inviteCode) LIMIT 1")
    suspend fun getTeamByInviteCodeOnce(inviteCode: String): Team?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: Team): Long

    @Update
    suspend fun updateTeam(team: Team)

    // --- Team Memberships ---
    @Query("SELECT * FROM team_memberships")
    fun getAllMemberships(): Flow<List<TeamMembership>>

    @Query("SELECT * FROM team_memberships")
    suspend fun getAllMembershipsOnce(): List<TeamMembership>

    @Query("SELECT * FROM team_memberships WHERE teamId = :teamId")
    fun getMembershipsByTeam(teamId: Long): Flow<List<TeamMembership>>

    @Query("SELECT * FROM team_memberships WHERE userId = :userId")
    fun getMembershipsByUser(userId: Long): Flow<List<TeamMembership>>

    @Query("SELECT * FROM team_memberships WHERE userId = :userId")
    suspend fun getMembershipsByUserOnce(userId: Long): List<TeamMembership>

    @Query("SELECT * FROM team_memberships WHERE userId = :userId AND teamId = :teamId LIMIT 1")
    suspend fun getMembershipOnce(userId: Long, teamId: Long): TeamMembership?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: TeamMembership): Long

    @Query("DELETE FROM team_memberships WHERE userId = :userId AND teamId = :teamId")
    suspend fun deleteMembership(userId: Long, teamId: Long)

    // --- Attendance ---
    @Query("SELECT * FROM attendances ORDER BY sessionDate DESC")
    fun getAllAttendances(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances ORDER BY sessionDate DESC")
    suspend fun getAllAttendancesOnce(): List<Attendance>

    @Query("SELECT * FROM attendances WHERE userId = :userId ORDER BY sessionDate DESC")
    fun getAttendancesByUser(userId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE teamId = :teamId ORDER BY sessionDate DESC")
    fun getAttendancesByTeam(teamId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE status = 'PENDING' ORDER BY sessionDate DESC")
    fun getPendingAttendances(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE teamId = :teamId AND status = 'APPROVED' AND sessionDate >= :startDate AND sessionDate <= :endDate")
    suspend fun getApprovedAttendancesForPeriod(teamId: Long, startDate: Long, endDate: Long): List<Attendance>

    @Query("SELECT * FROM attendances WHERE id = :id")
    suspend fun getAttendanceById(id: Long): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Query("UPDATE attendances SET status = :status, reviewedAt = :reviewedAt, reviewedByUserId = :reviewedBy, reviewNotes = :notes WHERE id = :id")
    suspend fun updateAttendanceStatus(id: Long, status: String, reviewedAt: Long, reviewedBy: Long, notes: String?)

    @Query("UPDATE attendances SET status = 'APPROVED', reviewedAt = :reviewedAt, reviewedByUserId = :reviewedBy WHERE status = 'PENDING'")
    suspend fun approveAllPendingAttendances(reviewedAt: Long, reviewedBy: Long)

    // --- Payments ---
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    suspend fun getAllPaymentsOnce(): List<Payment>

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY paymentDate DESC")
    fun getPaymentsByUser(userId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE status = 'PENDING' ORDER BY paymentDate DESC")
    fun getPendingPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): Payment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Query("UPDATE payments SET status = :status, reviewedAt = :reviewedAt, reviewedByUserId = :reviewedBy, reviewNotes = :notes WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, status: String, reviewedAt: Long, reviewedBy: Long, notes: String?)

    // --- Team Budgets ---
    @Query("SELECT * FROM team_budgets ORDER BY periodYear DESC, periodMonth DESC")
    fun getAllBudgets(): Flow<List<TeamBudget>>

    @Query("SELECT * FROM team_budgets ORDER BY periodYear DESC, periodMonth DESC")
    suspend fun getAllBudgetsOnce(): List<TeamBudget>

    @Query("SELECT * FROM team_budgets WHERE teamId = :teamId ORDER BY periodYear DESC, periodMonth DESC")
    fun getBudgetsByTeam(teamId: Long): Flow<List<TeamBudget>>

    @Query("SELECT * FROM team_budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): TeamBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: TeamBudget): Long

    @Update
    suspend fun updateBudget(budget: TeamBudget)

    // --- Invoices ---
    @Query("SELECT * FROM invoices ORDER BY issuedAt DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE budgetId = :budgetId")
    suspend fun getInvoiceByBudgetId(budgetId: Long): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    // --- Invoice Allocations ---
    @Query("SELECT * FROM invoice_allocations ORDER BY calculatedAt DESC")
    fun getAllAllocations(): Flow<List<InvoiceAllocation>>

    @Query("SELECT * FROM invoice_allocations WHERE invoiceId = :invoiceId")
    fun getAllocationsForInvoice(invoiceId: Long): Flow<List<InvoiceAllocation>>

    @Query("SELECT * FROM invoice_allocations WHERE invoiceId = :invoiceId")
    suspend fun getAllocationsForInvoiceOnce(invoiceId: Long): List<InvoiceAllocation>

    @Query("SELECT * FROM invoice_allocations WHERE userId = :userId ORDER BY calculatedAt DESC")
    fun getAllocationsByUser(userId: Long): Flow<List<InvoiceAllocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<InvoiceAllocation>)

    @Query("DELETE FROM invoice_allocations WHERE invoiceId = :invoiceId")
    suspend fun deleteAllocationsByInvoiceId(invoiceId: Long)

    // --- Balance Ledger ---
    @Query("SELECT * FROM balance_ledger ORDER BY createdAt DESC")
    fun getAllLedgerEntries(): Flow<List<BalanceLedger>>

    @Query("SELECT * FROM balance_ledger ORDER BY createdAt DESC")
    suspend fun getAllLedgerEntriesOnce(): List<BalanceLedger>

    @Query("SELECT * FROM balance_ledger WHERE userId = :userId ORDER BY createdAt DESC")
    fun getLedgerByUser(userId: Long): Flow<List<BalanceLedger>>

    @Query("SELECT * FROM balance_ledger WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestLedgerForUser(userId: Long): BalanceLedger?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: BalanceLedger): Long

    @Query("DELETE FROM balance_ledger WHERE referenceType = 'INVOICE_ALLOCATION' AND referenceId IN (SELECT id FROM invoice_allocations WHERE invoiceId = :invoiceId)")
    suspend fun deleteLedgerEntriesForInvoice(invoiceId: Long)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    // --- Disputes ---
    @Query("SELECT * FROM disputes ORDER BY createdAt DESC")
    fun getAllDisputes(): Flow<List<Dispute>>

    @Query("SELECT * FROM disputes WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDisputesByUser(userId: Long): Flow<List<Dispute>>

    @Query("SELECT * FROM disputes WHERE status = 'OPEN' ORDER BY createdAt DESC")
    fun getOpenDisputes(): Flow<List<Dispute>>

    @Query("SELECT * FROM disputes WHERE id = :id")
    suspend fun getDisputeById(id: Long): Dispute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispute(dispute: Dispute): Long

    @Update
    suspend fun updateDispute(dispute: Dispute)

    @Query("UPDATE disputes SET status = :status, resolvedAt = :resolvedAt, resolvedByUserId = :resolvedBy, resolutionNotes = :notes WHERE id = :id")
    suspend fun updateDisputeStatus(id: Long, status: String, resolvedAt: Long, resolvedBy: Long, notes: String)

    // --- Join Requests ---
    @Query("SELECT * FROM team_join_requests ORDER BY createdAt DESC")
    fun getAllJoinRequests(): Flow<List<TeamJoinRequest>>

    @Query("SELECT * FROM team_join_requests WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingJoinRequests(): Flow<List<TeamJoinRequest>>

    @Query("SELECT * FROM team_join_requests WHERE teamId = :teamId ORDER BY createdAt DESC")
    fun getJoinRequestsByTeam(teamId: Long): Flow<List<TeamJoinRequest>>

    @Query("SELECT * FROM team_join_requests WHERE id = :id")
    suspend fun getJoinRequestById(id: Long): TeamJoinRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoinRequest(request: TeamJoinRequest): Long

    @Update
    suspend fun updateJoinRequest(request: TeamJoinRequest)

    @Query("UPDATE team_join_requests SET status = :status, reviewedAt = :reviewedAt, reviewedByUserId = :reviewedBy, reviewNotes = :notes WHERE id = :id")
    suspend fun updateJoinRequestStatus(id: Long, status: String, reviewedAt: Long, reviewedBy: Long, notes: String?)
}

