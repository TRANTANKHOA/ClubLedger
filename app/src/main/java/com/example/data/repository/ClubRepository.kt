package com.example.data.repository

import com.example.data.dao.ClubDao
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ClubRepository(private val clubDao: ClubDao) {

    // --- Users & Teams ---
    val allUsers: Flow<List<User>> = clubDao.getAllUsers()
    val allMembers: Flow<List<User>> = clubDao.getMembers()
    val allTeams: Flow<List<Team>> = clubDao.getAllTeams()
    val allMemberships: Flow<List<TeamMembership>> = clubDao.getAllMemberships()
    val allBudgets: Flow<List<TeamBudget>> = clubDao.getAllBudgets()
    val allInvoices: Flow<List<Invoice>> = clubDao.getAllInvoices()
    val allAllocations: Flow<List<InvoiceAllocation>> = clubDao.getAllAllocations()
    val allAttendances: Flow<List<Attendance>> = clubDao.getAllAttendances()
    val pendingAttendances: Flow<List<Attendance>> = clubDao.getPendingAttendances()
    val allPayments: Flow<List<Payment>> = clubDao.getAllPayments()
    val pendingPayments: Flow<List<Payment>> = clubDao.getPendingPayments()
    val allLedgerEntries: Flow<List<BalanceLedger>> = clubDao.getAllLedgerEntries()
    val allAuditLogs: Flow<List<AuditLog>> = clubDao.getAllAuditLogs()
    val allDisputes: Flow<List<Dispute>> = clubDao.getAllDisputes()
    val openDisputes: Flow<List<Dispute>> = clubDao.getOpenDisputes()
    val allJoinRequests: Flow<List<TeamJoinRequest>> = clubDao.getAllJoinRequests()
    val pendingJoinRequests: Flow<List<TeamJoinRequest>> = clubDao.getPendingJoinRequests()

    fun getUserById(userId: Long): Flow<User?> = clubDao.getUserById(userId)
    fun getTeamById(teamId: Long): Flow<Team?> = clubDao.getTeamById(teamId)
    fun getTeamByInviteCode(inviteCode: String): Flow<Team?> = clubDao.getTeamByInviteCode(inviteCode)
    fun getMembershipsByTeam(teamId: Long): Flow<List<TeamMembership>> = clubDao.getMembershipsByTeam(teamId)
    fun getAttendancesByUser(userId: Long): Flow<List<Attendance>> = clubDao.getAttendancesByUser(userId)
    fun getPaymentsByUser(userId: Long): Flow<List<Payment>> = clubDao.getPaymentsByUser(userId)
    fun getLedgerByUser(userId: Long): Flow<List<BalanceLedger>> = clubDao.getLedgerByUser(userId)
    fun getAllocationsByUser(userId: Long): Flow<List<InvoiceAllocation>> = clubDao.getAllocationsByUser(userId)
    fun getAllocationsForInvoice(invoiceId: Long): Flow<List<InvoiceAllocation>> = clubDao.getAllocationsForInvoice(invoiceId)
    fun getDisputesByUser(userId: Long): Flow<List<Dispute>> = clubDao.getDisputesByUser(userId)

    // --- User Actions ---
    suspend fun createUser(user: User): Long {
        val id = clubDao.insertUser(user)
        clubDao.insertAuditLog(
            AuditLog(
                action = "USER_REGISTERED",
                entityType = "User",
                entityId = id,
                performedByUserId = id,
                details = "Registered member: ${user.name} (${user.email})"
            )
        )
        return id
    }

    suspend fun updateUser(user: User) {
        clubDao.updateUser(user)
    }

    suspend fun findUserByEmail(email: String): User? {
        if (email.isBlank()) return null
        return clubDao.getUserByEmailOnce(email.trim())
    }

    suspend fun createTeam(team: Team, adminId: Long): Long {
        val id = clubDao.insertTeam(team)
        clubDao.insertAuditLog(
            AuditLog(
                action = "TEAM_CREATED",
                entityType = "Team",
                entityId = id,
                performedByUserId = adminId,
                details = "Created team: ${team.name} (${team.sportType})"
            )
        )
        return id
    }

    suspend fun joinTeam(userId: Long, teamId: Long, role: String = "MEMBER") {
        val existing = clubDao.getMembershipOnce(userId, teamId)
        clubDao.insertMembership(
            TeamMembership(
                id = existing?.id ?: 0L,
                userId = userId,
                teamId = teamId,
                roleInTeam = role,
                joinedDate = existing?.joinedDate ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTeamMembershipRole(userId: Long, teamId: Long, newRole: String) {
        val user = clubDao.getUserByIdOnce(userId)
        val team = clubDao.getTeamByIdOnce(teamId)
        val existing = clubDao.getMembershipOnce(userId, teamId)
        clubDao.insertMembership(
            TeamMembership(
                id = existing?.id ?: 0L,
                userId = userId,
                teamId = teamId,
                roleInTeam = newRole,
                joinedDate = existing?.joinedDate ?: System.currentTimeMillis()
            )
        )
        clubDao.insertAuditLog(
            AuditLog(
                action = "TEAM_ROLE_UPDATED",
                entityType = "TeamMembership",
                entityId = teamId,
                performedByUserId = 1,
                details = "Updated ${user?.name ?: "User #$userId"} role in ${team?.name ?: "Team #$teamId"} to $newRole"
            )
        )
    }

    suspend fun updateUserRole(userId: Long, newRole: String) {
        val user = clubDao.getUserByIdOnce(userId) ?: return
        clubDao.updateUser(user.copy(role = newRole))
        clubDao.insertAuditLog(
            AuditLog(
                action = "USER_ROLE_UPDATED",
                entityType = "User",
                entityId = userId,
                performedByUserId = 1,
                details = "Updated user ${user.name} role to $newRole"
            )
        )
    }

    suspend fun syncUserAccess(sourceUserId: Long, targetUserId: Long) {
        val sourceUser = clubDao.getUserByIdOnce(sourceUserId) ?: return
        val targetUser = clubDao.getUserByIdOnce(targetUserId) ?: return

        // 1. Sync primary user role (e.g. ADMIN / MEMBER)
        if (targetUser.role != sourceUser.role) {
            clubDao.updateUser(targetUser.copy(role = sourceUser.role))
        }

        // 2. Sync all team memberships & team roles (e.g. CAPTAIN, MEMBER)
        val sourceMemberships = clubDao.getMembershipsByUserOnce(sourceUserId)
        val targetMemberships = clubDao.getMembershipsByUserOnce(targetUserId).associateBy { it.teamId }

        for (srcMem in sourceMemberships) {
            val existing = targetMemberships[srcMem.teamId]
            if (existing == null || existing.roleInTeam != srcMem.roleInTeam) {
                clubDao.insertMembership(
                    TeamMembership(
                        id = existing?.id ?: 0L,
                        userId = targetUserId,
                        teamId = srcMem.teamId,
                        roleInTeam = srcMem.roleInTeam,
                        joinedDate = existing?.joinedDate ?: System.currentTimeMillis()
                    )
                )
            }
        }

        clubDao.insertAuditLog(
            AuditLog(
                action = "PERMISSIONS_SYNCHRONIZED",
                entityType = "User",
                entityId = targetUserId,
                performedByUserId = 1,
                details = "Synchronized all roles and team access from ${sourceUser.name} to ${targetUser.name}"
            )
        )
    }

    suspend fun leaveTeam(userId: Long, teamId: Long) {
        clubDao.deleteMembership(userId, teamId)
    }

    // --- Attendance Operations ---
    suspend fun submitAttendance(attendance: Attendance): Long {
        return clubDao.insertAttendance(attendance)
    }

    suspend fun approveAttendance(attendanceId: Long, reviewerId: Long, notes: String? = null) {
        val att = clubDao.getAttendanceById(attendanceId) ?: return
        clubDao.updateAttendanceStatus(
            id = attendanceId,
            status = "APPROVED",
            reviewedAt = System.currentTimeMillis(),
            reviewedBy = reviewerId,
            notes = notes ?: "Approved by admin"
        )
        clubDao.insertAuditLog(
            AuditLog(
                action = "ATTENDANCE_APPROVED",
                entityType = "Attendance",
                entityId = attendanceId,
                performedByUserId = reviewerId,
                details = "Approved ${att.sessionType} session on ${formatDate(att.sessionDate)} for User #${att.userId}"
            )
        )
    }

    suspend fun rejectAttendance(attendanceId: Long, reviewerId: Long, reason: String) {
        val att = clubDao.getAttendanceById(attendanceId) ?: return
        clubDao.updateAttendanceStatus(
            id = attendanceId,
            status = "REJECTED",
            reviewedAt = System.currentTimeMillis(),
            reviewedBy = reviewerId,
            notes = reason
        )
        clubDao.insertAuditLog(
            AuditLog(
                action = "ATTENDANCE_REJECTED",
                entityType = "Attendance",
                entityId = attendanceId,
                performedByUserId = reviewerId,
                details = "Rejected ${att.sessionType} session for User #${att.userId}. Reason: $reason"
            )
        )
    }

    suspend fun bulkApproveAllPendingAttendances(reviewerId: Long) {
        val now = System.currentTimeMillis()
        clubDao.approveAllPendingAttendances(now, reviewerId)
        clubDao.insertAuditLog(
            AuditLog(
                action = "BULK_ATTENDANCE_APPROVED",
                entityType = "Attendance",
                entityId = 0,
                performedByUserId = reviewerId,
                details = "Bulk approved all pending attendance submissions."
            )
        )
    }

    // --- Payment Operations ---
    suspend fun submitPayment(payment: Payment): Long {
        return clubDao.insertPayment(payment)
    }

    suspend fun approvePayment(paymentId: Long, reviewerId: Long, notes: String? = null) {
        val payment = clubDao.getPaymentById(paymentId) ?: return
        val now = System.currentTimeMillis()

        clubDao.updatePaymentStatus(
            id = paymentId,
            status = "APPROVED",
            reviewedAt = now,
            reviewedBy = reviewerId,
            notes = notes ?: "Payment verified"
        )

        // Calculate running balance and update BalanceLedger
        val latestLedger = clubDao.getLatestLedgerForUser(payment.userId)
        val currentBalance = latestLedger?.runningBalanceAfter ?: 0.0
        val newBalance = currentBalance + payment.amount

        val ledgerEntry = BalanceLedger(
            userId = payment.userId,
            type = "PAYMENT_CREDIT",
            amount = payment.amount,
            runningBalanceAfter = newBalance,
            referenceType = "PAYMENT",
            referenceId = paymentId,
            description = "Payment received (${payment.paymentMethod}) - ${payment.referenceNote}",
            createdAt = now,
            createdByUserId = reviewerId
        )
        clubDao.insertLedgerEntry(ledgerEntry)

        clubDao.insertAuditLog(
            AuditLog(
                action = "PAYMENT_APPROVED",
                entityType = "Payment",
                entityId = paymentId,
                performedByUserId = reviewerId,
                details = "Approved $${String.format(Locale.US, "%.2f", payment.amount)} from User #${payment.userId}. New Balance: $${String.format(Locale.US, "%.2f", newBalance)}"
            )
        )
    }

    suspend fun rejectPayment(paymentId: Long, reviewerId: Long, reason: String) {
        val payment = clubDao.getPaymentById(paymentId) ?: return
        clubDao.updatePaymentStatus(
            id = paymentId,
            status = "REJECTED",
            reviewedAt = System.currentTimeMillis(),
            reviewedBy = reviewerId,
            notes = reason
        )
        clubDao.insertAuditLog(
            AuditLog(
                action = "PAYMENT_REJECTED",
                entityType = "Payment",
                entityId = paymentId,
                performedByUserId = reviewerId,
                details = "Rejected $${String.format(Locale.US, "%.2f", payment.amount)} payment for User #${payment.userId}. Reason: $reason"
            )
        )
    }

    // --- Budget & Invoicing Allocation Engine ---
    suspend fun createBudget(budget: TeamBudget, adminId: Long): Long {
        val id = clubDao.insertBudget(budget)
        clubDao.insertAuditLog(
            AuditLog(
                action = "BUDGET_CREATED",
                entityType = "TeamBudget",
                entityId = id,
                performedByUserId = adminId,
                details = "Created budget '${budget.title}' for $${String.format(Locale.US, "%.2f", budget.totalAmount)} (${budget.periodMonth}/${budget.periodYear})"
            )
        )
        return id
    }

    suspend fun generateInvoiceAndAllocate(
        budgetId: Long,
        adminId: Long
    ): Result<Invoice> {
        val budget = clubDao.getBudgetById(budgetId) ?: return Result.failure(Exception("Budget not found"))
        val team = clubDao.getTeamByIdOnce(budget.teamId) ?: return Result.failure(Exception("Team not found"))

        // Calculate time range for the periodMonth / periodYear
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, budget.periodYear)
        cal.set(Calendar.MONTH, budget.periodMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startDate = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endDate = cal.timeInMillis

        val approvedAttendances = clubDao.getApprovedAttendancesForPeriod(budget.teamId, startDate, endDate)
        if (approvedAttendances.isEmpty()) {
            return Result.failure(Exception("No approved attendance records found for ${team.name} in ${budget.periodMonth}/${budget.periodYear}. Please approve member attendances first."))
        }

        val attendanceByUser = approvedAttendances.groupBy { it.userId }
        val totalSessions = approvedAttendances.size
        val costPerSession = if (totalSessions > 0) budget.totalAmount / totalSessions else 0.0

        val invoiceNumber = "INV-${budget.periodYear}-${budget.periodMonth}-${team.name.take(3).uppercase()}${System.currentTimeMillis() % 1000}"
        val invoice = Invoice(
            budgetId = budget.id,
            teamId = budget.teamId,
            invoiceNumber = invoiceNumber,
            title = "Monthly Allocation - ${budget.title}",
            totalAmount = budget.totalAmount,
            periodMonth = budget.periodMonth,
            periodYear = budget.periodYear,
            category = budget.category,
            attachmentUrl = budget.attachmentUrl,
            attachmentType = budget.attachmentType,
            attachmentName = budget.attachmentName,
            status = "ISSUED",
            issuedAt = System.currentTimeMillis(),
            totalApprovedSessions = totalSessions,
            costPerSession = costPerSession
        )
        val invoiceId = clubDao.insertInvoice(invoice)
        val savedInvoice = invoice.copy(id = invoiceId)

        // Generate proportional allocations
        val allocations = mutableListOf<InvoiceAllocation>()
        var allocatedSum = 0.0

        val userList = attendanceByUser.keys.toList()
        for (i in userList.indices) {
            val uId = userList[i]
            val userSessions = attendanceByUser[uId]?.size ?: 0
            val percentage = (userSessions.toDouble() / totalSessions.toDouble()) * 100.0

            // Exact rounded cent allocation
            val rawAmount = (userSessions.toDouble() / totalSessions.toDouble()) * budget.totalAmount
            val roundedAmount = (rawAmount * 100.0).roundToInt() / 100.0

            allocations.add(
                InvoiceAllocation(
                    invoiceId = invoiceId,
                    userId = uId,
                    teamId = budget.teamId,
                    approvedSessionsCount = userSessions,
                    percentage = (percentage * 100.0).roundToInt() / 100.0,
                    allocatedAmount = roundedAmount,
                    status = "APPLIED_TO_LEDGER",
                    calculatedAt = System.currentTimeMillis()
                )
            )
            allocatedSum += roundedAmount
        }

        // Adjust cents rounding difference if any
        val diff = ((budget.totalAmount - allocatedSum) * 100.0).roundToInt() / 100.0
        if (diff != 0.0 && allocations.isNotEmpty()) {
            val first = allocations[0]
            allocations[0] = first.copy(allocatedAmount = ((first.allocatedAmount + diff) * 100.0).roundToInt() / 100.0)
        }

        // Insert allocations and write back the generated IDs so ledger entries
        // carry a referenceId that deleteLedgerEntriesForInvoice() can match on recalculation.
        val insertedIds = clubDao.insertAllocations(allocations)
        for (i in allocations.indices) {
            allocations[i] = allocations[i].copy(id = insertedIds[i])
        }

        // Post debits to BalanceLedger for each member
        val now = System.currentTimeMillis()
        for (alloc in allocations) {
            val latest = clubDao.getLatestLedgerForUser(alloc.userId)
            val currentBal = latest?.runningBalanceAfter ?: 0.0
            val newBal = currentBal - alloc.allocatedAmount

            clubDao.insertLedgerEntry(
                BalanceLedger(
                    userId = alloc.userId,
                    type = "INVOICE_DEBIT",
                    amount = -alloc.allocatedAmount,
                    runningBalanceAfter = newBal,
                    referenceType = "INVOICE_ALLOCATION",
                    referenceId = alloc.id,
                    description = "Allocated cost for ${invoice.invoiceNumber} (${alloc.approvedSessionsCount} sessions, ${alloc.percentage}%)",
                    createdAt = now,
                    createdByUserId = adminId
                )
            )
        }

        // Update budget status
        clubDao.updateBudget(budget.copy(status = "INVOICED"))

        clubDao.insertAuditLog(
            AuditLog(
                action = "BUDGET_ALLOCATED",
                entityType = "Invoice",
                entityId = invoiceId,
                performedByUserId = adminId,
                details = "Allocated $${String.format(Locale.US, "%.2f", budget.totalAmount)} across $totalSessions sessions to ${allocations.size} members for ${team.name}."
            )
        )

        return Result.success(savedInvoice)
    }

    suspend fun recalculateInvoice(invoiceId: Long, adminId: Long): Result<Invoice> {
        val invoice = clubDao.getInvoiceById(invoiceId) ?: return Result.failure(Exception("Invoice not found"))
        val budget = clubDao.getBudgetById(invoice.budgetId) ?: return Result.failure(Exception("Budget not found"))
        val team = clubDao.getTeamByIdOnce(invoice.teamId) ?: return Result.failure(Exception("Team not found"))

        // Calculate time range
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, invoice.periodYear)
        cal.set(Calendar.MONTH, invoice.periodMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startDate = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endDate = cal.timeInMillis

        val approvedAttendances = clubDao.getApprovedAttendancesForPeriod(invoice.teamId, startDate, endDate)
        if (approvedAttendances.isEmpty()) {
            return Result.failure(Exception("No approved attendances for period."))
        }

        val attendanceByUser = approvedAttendances.groupBy { it.userId }
        val totalSessions = approvedAttendances.size
        val costPerSession = budget.totalAmount / totalSessions

        // Delete old ledger entries and allocations
        clubDao.deleteLedgerEntriesForInvoice(invoiceId)
        clubDao.deleteAllocationsByInvoiceId(invoiceId)

        // Generate new allocations
        val allocations = mutableListOf<InvoiceAllocation>()
        var allocatedSum = 0.0

        val userList = attendanceByUser.keys.toList()
        for (uId in userList) {
            val userSessions = attendanceByUser[uId]?.size ?: 0
            val percentage = (userSessions.toDouble() / totalSessions.toDouble()) * 100.0
            val rawAmount = (userSessions.toDouble() / totalSessions.toDouble()) * budget.totalAmount
            val roundedAmount = (rawAmount * 100.0).roundToInt() / 100.0

            allocations.add(
                InvoiceAllocation(
                    invoiceId = invoiceId,
                    userId = uId,
                    teamId = invoice.teamId,
                    approvedSessionsCount = userSessions,
                    percentage = (percentage * 100.0).roundToInt() / 100.0,
                    allocatedAmount = roundedAmount,
                    status = "APPLIED_TO_LEDGER",
                    calculatedAt = System.currentTimeMillis()
                )
            )
            allocatedSum += roundedAmount
        }

        val diff = ((budget.totalAmount - allocatedSum) * 100.0).roundToInt() / 100.0
        if (diff != 0.0 && allocations.isNotEmpty()) {
            val first = allocations[0]
            allocations[0] = first.copy(allocatedAmount = ((first.allocatedAmount + diff) * 100.0).roundToInt() / 100.0)
        }

        // Insert allocations and write back the generated IDs so ledger entries
        // carry a referenceId that deleteLedgerEntriesForInvoice() can match on recalculation.
        val insertedIds = clubDao.insertAllocations(allocations)
        for (i in allocations.indices) {
            allocations[i] = allocations[i].copy(id = insertedIds[i])
        }

        // Post revised ledger entries
        val now = System.currentTimeMillis()
        for (alloc in allocations) {
            val latest = clubDao.getLatestLedgerForUser(alloc.userId)
            val currentBal = latest?.runningBalanceAfter ?: 0.0
            val newBal = currentBal - alloc.allocatedAmount

            clubDao.insertLedgerEntry(
                BalanceLedger(
                    userId = alloc.userId,
                    type = "INVOICE_DEBIT",
                    amount = -alloc.allocatedAmount,
                    runningBalanceAfter = newBal,
                    referenceType = "INVOICE_ALLOCATION",
                    referenceId = alloc.id,
                    description = "Recalculated cost for ${invoice.invoiceNumber} (${alloc.approvedSessionsCount} sessions, ${alloc.percentage}%)",
                    createdAt = now,
                    createdByUserId = adminId
                )
            )
        }

        val updatedInvoice = invoice.copy(
            totalApprovedSessions = totalSessions,
            costPerSession = costPerSession
        )
        clubDao.updateInvoice(updatedInvoice)

        clubDao.insertAuditLog(
            AuditLog(
                action = "ALLOCATION_RECALCULATED",
                entityType = "Invoice",
                entityId = invoiceId,
                performedByUserId = adminId,
                details = "Recalculated allocations for ${invoice.invoiceNumber} with $totalSessions updated sessions."
            )
        )

        return Result.success(updatedInvoice)
    }

    // --- Manual Balance Adjustments ---
    suspend fun applyManualBalanceAdjustment(
        userId: Long,
        adjustmentType: String, // "CREDIT", "PENALTY", "REFUND", "CORRECTION"
        amount: Double,
        reason: String,
        adminId: Long
    ) {
        val signedAmount = when (adjustmentType) {
            "CREDIT", "REFUND" -> amount
            "PENALTY" -> -amount
            "CORRECTION" -> amount // can be positive or negative
            else -> amount
        }

        val latest = clubDao.getLatestLedgerForUser(userId)
        val currentBalance = latest?.runningBalanceAfter ?: 0.0
        val newBalance = currentBalance + signedAmount

        val ledgerEntry = BalanceLedger(
            userId = userId,
            type = "MANUAL_ADJUSTMENT",
            amount = signedAmount,
            runningBalanceAfter = newBalance,
            referenceType = "ADMIN_ADJUSTMENT",
            referenceId = 0,
            description = "[$adjustmentType] $reason",
            createdAt = System.currentTimeMillis(),
            createdByUserId = adminId
        )
        clubDao.insertLedgerEntry(ledgerEntry)

        clubDao.insertAuditLog(
            AuditLog(
                action = "MANUAL_BALANCE_ADJUSTED",
                entityType = "User",
                entityId = userId,
                performedByUserId = adminId,
                details = "Manual $adjustmentType of $${String.format(Locale.US, "%.2f", signedAmount)} for User #$userId. Reason: $reason. New balance: $${String.format(Locale.US, "%.2f", newBalance)}"
            )
        )
    }

    // --- Team Owner Fast Batch Attendance Marking ---
    suspend fun recordBatchOwnerAttendance(
        teamId: Long,
        sessionDate: Long,
        sessionType: String,
        notes: String,
        presentUserIds: List<Long>,
        adminId: Long
    ): Int {
        val now = System.currentTimeMillis()
        var count = 0
        for (uId in presentUserIds) {
            val att = Attendance(
                userId = uId,
                teamId = teamId,
                sessionDate = sessionDate,
                sessionType = sessionType,
                notes = if (notes.isBlank()) "Marked present by team owner" else notes,
                status = "APPROVED",
                reviewedAt = now,
                reviewedByUserId = adminId,
                reviewNotes = "Verified directly by team owner"
            )
            clubDao.insertAttendance(att)
            count++
        }

        val team = clubDao.getTeamByIdOnce(teamId)
        clubDao.insertAuditLog(
            AuditLog(
                action = "BATCH_ATTENDANCE_RECORDED",
                entityType = "Attendance",
                entityId = teamId,
                performedByUserId = adminId,
                details = "Team owner recorded and approved $sessionType attendance for $count players on ${formatDate(sessionDate)} for ${team?.name ?: "Team #$teamId"}."
            )
        )
        return count
    }

    // --- Team Owner Payment Request / Dues Issuance ---
    suspend fun issuePaymentRequest(
        userIds: List<Long>,
        title: String,
        amount: Double,
        memo: String,
        adminId: Long
    ): Int {
        val now = System.currentTimeMillis()
        var count = 0
        for (uId in userIds) {
            val latest = clubDao.getLatestLedgerForUser(uId)
            val currentBalance = latest?.runningBalanceAfter ?: 0.0
            val newBalance = currentBalance - amount

            val ledgerEntry = BalanceLedger(
                userId = uId,
                type = "PAYMENT_REQUEST",
                amount = -amount,
                runningBalanceAfter = newBalance,
                referenceType = "PAYMENT_REQUEST",
                referenceId = 0,
                description = "Payment Request: $title${if (memo.isNotBlank()) " ($memo)" else ""}",
                createdAt = now,
                createdByUserId = adminId
            )
            clubDao.insertLedgerEntry(ledgerEntry)
            count++
        }

        clubDao.insertAuditLog(
            AuditLog(
                action = "PAYMENT_REQUEST_ISSUED",
                entityType = "BalanceLedger",
                entityId = 0,
                performedByUserId = adminId,
                details = "Issued '$title' payment request ($${String.format(Locale.US, "%.2f", amount)}) to $count member(s). Memo: $memo"
            )
        )
        return count
    }

    // --- Export CSV Generators ---
    fun generateAttendanceCsv(attendances: List<Attendance>, users: Map<Long, User>, teams: Map<Long, Team>): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Member Name,Member Email,Team,Session Type,Status,Notes,Review Notes\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (att in attendances) {
            val user = users[att.userId]
            val team = teams[att.teamId]
            sb.append("${att.id},")
            sb.append("\"${sdf.format(Date(att.sessionDate))}\",")
            sb.append("\"${user?.name ?: "Unknown"}\",")
            sb.append("\"${user?.email ?: ""}\",")
            sb.append("\"${team?.name ?: "Unknown"}\",")
            sb.append("\"${att.sessionType}\",")
            sb.append("\"${att.status}\",")
            sb.append("\"${att.notes.replace("\"", "\"\"")}\",")
            sb.append("\"${(att.reviewNotes ?: "").replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun generatePaymentsCsv(payments: List<Payment>, users: Map<Long, User>): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Member Name,Amount,Method,Status,Reference Note,Receipt Note,Review Notes\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (p in payments) {
            val user = users[p.userId]
            sb.append("${p.id},")
            sb.append("\"${sdf.format(Date(p.paymentDate))}\",")
            sb.append("\"${user?.name ?: "Unknown"}\",")
            sb.append("${p.amount},")
            sb.append("\"${p.paymentMethod}\",")
            sb.append("\"${p.status}\",")
            sb.append("\"${p.referenceNote.replace("\"", "\"\"")}\",")
            sb.append("\"${(p.receiptNote ?: "").replace("\"", "\"\"")}\",")
            sb.append("\"${(p.reviewNotes ?: "").replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    // --- Dispute Management ---
    suspend fun raiseDispute(
        userId: Long,
        category: String,
        referenceType: String,
        referenceId: Long,
        title: String,
        description: String,
        requestedAdjustmentAmount: Double
    ): Long {
        val dispute = Dispute(
            userId = userId,
            category = category,
            referenceType = referenceType,
            referenceId = referenceId,
            title = title,
            description = description,
            requestedAdjustmentAmount = requestedAdjustmentAmount,
            status = "OPEN",
            createdAt = System.currentTimeMillis()
        )
        val disputeId = clubDao.insertDispute(dispute)
        val user = clubDao.getUserByIdOnce(userId)

        clubDao.insertAuditLog(
            AuditLog(
                action = "DISPUTE_RAISED",
                entityType = "Dispute",
                entityId = disputeId,
                performedByUserId = userId,
                details = "Member ${user?.name ?: "User #$userId"} raised dispute on $referenceType #$referenceId: \"$title\" (${category.replace("_", " ")})"
            )
        )
        return disputeId
    }

    suspend fun resolveDispute(
        disputeId: Long,
        adminId: Long,
        resolutionAction: String, // "CREDIT_AND_RESOLVE", "EXPLAIN_AND_RESOLVE", "DISMISS"
        resolutionNotes: String,
        creditAmount: Double = 0.0
    ) {
        val dispute = clubDao.getDisputeById(disputeId) ?: return
        val now = System.currentTimeMillis()
        val user = clubDao.getUserByIdOnce(dispute.userId)
        val admin = clubDao.getUserByIdOnce(adminId)

        val newStatus = when (resolutionAction) {
            "CREDIT_AND_RESOLVE" -> "RESOLVED_CREDITED"
            "EXPLAIN_AND_RESOLVE" -> "RESOLVED_EXPLAINED"
            else -> "DISMISSED"
        }

        // If financial credit requested/approved, apply an offsetting ledger adjustment
        if (resolutionAction == "CREDIT_AND_RESOLVE" && creditAmount > 0.0) {
            val latest = clubDao.getLatestLedgerForUser(dispute.userId)
            val currentBalance = latest?.runningBalanceAfter ?: 0.0
            val newBalance = currentBalance + creditAmount

            val ledger = BalanceLedger(
                userId = dispute.userId,
                type = "REFUND",
                amount = creditAmount,
                runningBalanceAfter = newBalance,
                referenceType = "DISPUTE_RESOLUTION",
                referenceId = disputeId,
                description = "Dispute #${dispute.id} Resolved: $resolutionNotes (Credit +$${String.format(Locale.US, "%.2f", creditAmount)})",
                createdAt = now,
                createdByUserId = adminId
            )
            clubDao.insertLedgerEntry(ledger)
        }

        clubDao.updateDisputeStatus(
            id = disputeId,
            status = newStatus,
            resolvedAt = now,
            resolvedBy = adminId,
            notes = resolutionNotes
        )

        clubDao.insertAuditLog(
            AuditLog(
                action = "DISPUTE_RESOLVED",
                entityType = "Dispute",
                entityId = disputeId,
                performedByUserId = adminId,
                details = "Admin ${admin?.name ?: "Admin #$adminId"} resolved Dispute #${dispute.id} for ${user?.name ?: "User #$dispute.userId"}. Status: $newStatus. Notes: $resolutionNotes"
            )
        )
    }

    suspend fun findTeamByCode(codeOrLink: String): Team? {
        val cleanCode = codeOrLink.trim()
            .substringAfter("code=")
            .substringAfter("team=")
            .substringAfterLast("/")
            .trim()
        if (cleanCode.isBlank()) return null
        return clubDao.getTeamByInviteCodeOnce(cleanCode)
    }

    suspend fun submitJoinRequest(
        teamCodeOrLink: String,
        applicantName: String,
        applicantEmail: String,
        applicantPhone: String,
        message: String,
        existingUserId: Long?
    ): Result<TeamJoinRequest> {
        val targetTeam = findTeamByCode(teamCodeOrLink)
            ?: return Result.failure(Exception("Invalid invite link or team code. Please verify and try again."))

        val request = TeamJoinRequest(
            teamId = targetTeam.id,
            applicantName = applicantName.trim(),
            applicantEmail = applicantEmail.trim(),
            applicantPhone = applicantPhone.trim(),
            message = message.trim(),
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            existingUserId = existingUserId
        )
        val id = clubDao.insertJoinRequest(request)
        val saved = request.copy(id = id)

        clubDao.insertAuditLog(
            AuditLog(
                action = "JOIN_REQUEST_SUBMITTED",
                entityType = "TeamJoinRequest",
                entityId = id,
                performedByUserId = existingUserId ?: 0,
                details = "$applicantName requested to join team ${targetTeam.name} via invite link (${targetTeam.inviteCode})"
            )
        )
        return Result.success(saved)
    }

    suspend fun approveJoinRequest(requestId: Long, adminId: Long, notes: String? = null): Result<User> {
        val req = clubDao.getJoinRequestById(requestId)
            ?: return Result.failure(Exception("Join request #$requestId not found."))
        val targetTeam = clubDao.getTeamByIdOnce(req.teamId)
            ?: return Result.failure(Exception("Target team #${req.teamId} not found."))
        val admin = clubDao.getUserByIdOnce(adminId)

        val now = System.currentTimeMillis()

        // 1. Resolve or create user
        val targetUser: User = if (req.existingUserId != null) {
            val existing = clubDao.getUserByIdOnce(req.existingUserId)
            if (existing != null) {
                if (existing.status != "ACTIVE") {
                    val updated = existing.copy(status = "ACTIVE")
                    clubDao.updateUser(updated)
                    updated
                } else existing
            } else {
                val newUser = User(
                    name = req.applicantName,
                    email = req.applicantEmail,
                    phone = req.applicantPhone,
                    role = "MEMBER",
                    avatarColorHex = 0xFF00897B,
                    joinedDate = now,
                    status = "ACTIVE"
                )
                val newId = clubDao.insertUser(newUser)
                newUser.copy(id = newId)
            }
        } else {
            // Check if user with same email exists
            val allUsers = clubDao.getAllUsersOnce()
            val existingByEmail = allUsers.firstOrNull { it.email.equals(req.applicantEmail, ignoreCase = true) }
            if (existingByEmail != null) {
                existingByEmail
            } else {
                val newUser = User(
                    name = req.applicantName,
                    email = req.applicantEmail,
                    phone = req.applicantPhone,
                    role = "MEMBER",
                    avatarColorHex = 0xFF00897B,
                    joinedDate = now,
                    status = "ACTIVE"
                )
                val newId = clubDao.insertUser(newUser)
                newUser.copy(id = newId)
            }
        }

        // 2. Add Team Membership
        clubDao.insertMembership(
            TeamMembership(
                userId = targetUser.id,
                teamId = targetTeam.id,
                roleInTeam = "MEMBER",
                joinedDate = now
            )
        )

        // 3. Update Join Request Status
        clubDao.updateJoinRequestStatus(
            id = requestId,
            status = "APPROVED",
            reviewedAt = now,
            reviewedBy = adminId,
            notes = notes ?: "Approved by ${admin?.name ?: "Club Owner"}"
        )

        // 4. Audit Log
        clubDao.insertAuditLog(
            AuditLog(
                action = "JOIN_REQUEST_APPROVED",
                entityType = "TeamJoinRequest",
                entityId = requestId,
                performedByUserId = adminId,
                details = "Admin ${admin?.name ?: "Admin #$adminId"} approved ${targetUser.name} to join team ${targetTeam.name}"
            )
        )

        return Result.success(targetUser)
    }

    suspend fun rejectJoinRequest(requestId: Long, adminId: Long, reason: String): Result<Unit> {
        val req = clubDao.getJoinRequestById(requestId)
            ?: return Result.failure(Exception("Join request #$requestId not found."))
        val targetTeam = clubDao.getTeamByIdOnce(req.teamId)
        val admin = clubDao.getUserByIdOnce(adminId)
        val now = System.currentTimeMillis()

        clubDao.updateJoinRequestStatus(
            id = requestId,
            status = "REJECTED",
            reviewedAt = now,
            reviewedBy = adminId,
            notes = reason
        )

        clubDao.insertAuditLog(
            AuditLog(
                action = "JOIN_REQUEST_REJECTED",
                entityType = "TeamJoinRequest",
                entityId = requestId,
                performedByUserId = adminId,
                details = "Admin ${admin?.name ?: "Admin #$adminId"} rejected join request from ${req.applicantName} for ${targetTeam?.name ?: "Team"}. Reason: $reason"
            )
        )

        return Result.success(Unit)
    }

    fun generateLedgerCsv(entries: List<BalanceLedger>, users: Map<Long, User>): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Member Name,Type,Amount,Running Balance,Reference Type,Description\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        for (e in entries) {
            val user = users[e.userId]
            sb.append("${e.id},")
            sb.append("\"${sdf.format(Date(e.createdAt))}\",")
            sb.append("\"${user?.name ?: "Unknown"}\",")
            sb.append("\"${e.type}\",")
            sb.append("${e.amount},")
            sb.append("${e.runningBalanceAfter},")
            sb.append("\"${e.referenceType}\",")
            sb.append("\"${e.description.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        return sdf.format(Date(millis))
    }
}
