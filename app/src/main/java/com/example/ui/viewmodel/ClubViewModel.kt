package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ClubDatabase
import com.example.data.entity.*
import com.example.data.model.PermissionEngine
import com.example.data.model.RoleConstants
import com.example.data.model.UserRoleProfile
import com.example.data.remote.AuthState
import com.example.data.remote.CloudSyncState
import com.example.data.remote.FirebaseAuthManager
import com.example.data.remote.FirestoreSyncManager
import com.example.data.repository.ClubRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class MemberSummary(
    val user: User,
    val balance: Double, // >0 credit, <0 owes, =0 paid
    val totalApprovedSessions: Int,
    val pendingSessions: Int,
    val totalApprovedPaid: Double,
    val totalAllocatedCost: Double,
    val statusText: String // "Paid", "Overpaid ($X.XX credit)", "Owing $X.XX"
)

data class TeamDashboardStats(
    val team: Team,
    val memberCount: Int,
    val totalApprovedSessionsThisMonth: Int,
    val totalBudgetValue: Double,
    val totalCollected: Double,
    val totalOutstanding: Double
)

class ClubViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClubRepository
    private val firestoreSyncManager: FirestoreSyncManager
    private val authManager: FirebaseAuthManager

    init {
        val db = ClubDatabase.getDatabase(application, viewModelScope)
        repository = ClubRepository(db.clubDao())
        firestoreSyncManager = FirestoreSyncManager(application, db.clubDao(), viewModelScope)
        authManager = FirebaseAuthManager(application)

        // Ensure Marcus has all access, team memberships, and roles that Sarah has
        viewModelScope.launch {
            repository.syncUserAccess(sourceUserId = 3, targetUserId = 4)
        }
    }

    // Cloud Multi-User Sync & Auth States
    val cloudSyncState: StateFlow<CloudSyncState> = firestoreSyncManager.syncState
    val authState: StateFlow<AuthState> = authManager.authState
    fun isFirebaseConfigured(): Boolean = firestoreSyncManager.isFirebaseConfigured()

    fun enableCloudSync(clubId: String) {
        firestoreSyncManager.enableCloudSync(clubId)
        _snackbarMessage.value = "Cloud Sync enabled for Club ID: $clubId"
    }

    fun disableCloudSync() {
        firestoreSyncManager.disableCloudSync()
        _snackbarMessage.value = "Cloud Sync disabled (Local Mode Active)"
    }

    fun syncAllToCloud() {
        viewModelScope.launch {
            firestoreSyncManager.pushAllLocalToCloud()
            _snackbarMessage.value = "Synced local records to cloud."
        }
    }

    fun signInWithGoogle(activity: android.app.Activity? = null, webClientId: String? = null) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(activity, webClientId)
            if (result.isSuccess) {
                val fbUser = result.getOrNull()
                fbUser?.let { syncCloudUserToLocalProfile(it) }
                _snackbarMessage.value = "Signed in with Google as ${fbUser?.email ?: "User"}"
            } else {
                val err = result.exceptionOrNull()
                if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    _snackbarMessage.value = "Google Sign-In: ${err?.localizedMessage}"
                }
            }
        }
    }

    fun signInWithApple(activity: android.app.Activity) {
        viewModelScope.launch {
            val result = authManager.signInWithApple(activity)
            if (result.isSuccess) {
                val fbUser = result.getOrNull()
                fbUser?.let { syncCloudUserToLocalProfile(it) }
                _snackbarMessage.value = "Signed in with Apple as ${fbUser?.email ?: "Apple User"}"
            } else {
                _snackbarMessage.value = "Apple Sign-In: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun signInWithFacebook(activity: android.app.Activity) {
        viewModelScope.launch {
            val result = authManager.signInWithFacebook(activity)
            if (result.isSuccess) {
                val fbUser = result.getOrNull()
                fbUser?.let { syncCloudUserToLocalProfile(it) }
                _snackbarMessage.value = "Signed in with Facebook as ${fbUser?.displayName ?: "Facebook User"}"
            } else {
                _snackbarMessage.value = "Facebook Sign-In: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun signOutCloud() {
        authManager.signOut()
        firestoreSyncManager.disableCloudSync()
        _snackbarMessage.value = "Signed out of Cloud Authentication."
    }

    private suspend fun syncCloudUserToLocalProfile(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val email = firebaseUser.email
        if (!email.isNullOrBlank()) {
            val existing = repository.findUserByEmail(email)
            if (existing != null) {
                _currentUser.value = existing
            } else {
                val displayName = firebaseUser.displayName?.ifBlank { null } ?: email.substringBefore("@")
                val newUserId = repository.createUser(
                    User(
                        name = displayName,
                        email = email,
                        phone = firebaseUser.phoneNumber ?: "",
                        role = "MEMBER",
                        status = "ACTIVE"
                    )
                )
                val createdUser = repository.findUserByEmail(email)
                if (createdUser != null) {
                    _currentUser.value = createdUser
                }
            }
        }
    }

    // All entity flows
    val allUsers: StateFlow<List<User>> = repository.allUsers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allMembers: StateFlow<List<User>> = repository.allMembers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allTeams: StateFlow<List<Team>> = repository.allTeams.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allMemberships: StateFlow<List<TeamMembership>> = repository.allMemberships.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allBudgets: StateFlow<List<TeamBudget>> = repository.allBudgets.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allInvoices: StateFlow<List<Invoice>> = repository.allInvoices.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allAllocations: StateFlow<List<InvoiceAllocation>> = repository.allAllocations.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allAttendances: StateFlow<List<Attendance>> = repository.allAttendances.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val pendingAttendances: StateFlow<List<Attendance>> = repository.pendingAttendances.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allPayments: StateFlow<List<Payment>> = repository.allPayments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val pendingPayments: StateFlow<List<Payment>> = repository.pendingPayments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allLedgerEntries: StateFlow<List<BalanceLedger>> = repository.allLedgerEntries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allDisputes: StateFlow<List<Dispute>> = repository.allDisputes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val openDisputes: StateFlow<List<Dispute>> = repository.openDisputes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allJoinRequests: StateFlow<List<TeamJoinRequest>> = repository.allJoinRequests.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val pendingJoinRequests: StateFlow<List<TeamJoinRequest>> = repository.pendingJoinRequests.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Current active user for role simulation / login state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Notification / Snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            allUsers.collect { users ->
                if (_currentUser.value == null && users.isNotEmpty()) {
                    // Default to Alex Chen (Member) or Dave Miller (Admin)
                    _currentUser.value = users.firstOrNull { it.name.contains("Alex") } ?: users.first()
                }
            }
        }
    }

    fun switchUser(user: User) {
        _currentUser.value = user
        _snackbarMessage.value = "Switched view to ${user.name} (${user.role})"
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Member specific computed balance and stats
    val currentUserBalance = combine(
        currentUser,
        allLedgerEntries
    ) { user, ledger ->
        if (user == null) 0.0
        else {
            val userEntries = ledger.filter { it.userId == user.id }
            userEntries.maxByOrNull { it.createdAt }?.runningBalanceAfter ?: 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentUserAttendances = combine(
        currentUser,
        allAttendances
    ) { user, attendances ->
        if (user == null) emptyList()
        else attendances.filter { it.userId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserPayments = combine(
        currentUser,
        allPayments
    ) { user, payments ->
        if (user == null) emptyList()
        else payments.filter { it.userId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserAllocations = combine(
        currentUser,
        allAllocations
    ) { user, allocations ->
        if (user == null) emptyList()
        else allocations.filter { it.userId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserLedger = combine(
        currentUser,
        allLedgerEntries
    ) { user, ledger ->
        if (user == null) emptyList()
        else ledger.filter { it.userId == user.id }.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserDisputes = combine(
        currentUser,
        allDisputes
    ) { user, disputes ->
        if (user == null) emptyList()
        else disputes.filter { it.userId == user.id }.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserMemberships = combine(
        currentUser,
        allMemberships
    ) { user, memberships ->
        if (user == null) emptyList()
        else memberships.filter { it.userId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserIsGlobalAdmin = currentUser.map { user ->
        PermissionEngine.isGlobalAdmin(user)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentUserHasLeadershipRole = combine(
        currentUser,
        allMemberships
    ) { user, memberships ->
        PermissionEngine.hasAnyLeadershipRole(user, memberships)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentUserLedTeamIds = combine(
        currentUser,
        allMemberships,
        allTeams
    ) { user, memberships, teams ->
        PermissionEngine.getLedTeamIds(user, memberships, teams.map { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Club-wide Member summaries for Admin
    val memberSummaries: StateFlow<List<MemberSummary>> = combine(
        allUsers,
        allLedgerEntries,
        allAttendances,
        allPayments,
        allAllocations
    ) { users, ledger, attendances, payments, allocations ->
        users.filter { it.role == "MEMBER" }.map { user ->
            val userLedger = ledger.filter { it.userId == user.id }
            val latestBalance = userLedger.maxByOrNull { it.createdAt }?.runningBalanceAfter ?: 0.0
            val userAtt = attendances.filter { it.userId == user.id }
            val approvedCount = userAtt.count { it.status == "APPROVED" }
            val pendingCount = userAtt.count { it.status == "PENDING" }
            val approvedPaid = payments.filter { it.userId == user.id && it.status == "APPROVED" }.sumOf { it.amount }
            val totalAllocated = allocations.filter { it.userId == user.id }.sumOf { it.allocatedAmount }

            val statusText = when {
                latestBalance > 0.01 -> "Overpaid ($${String.format(java.util.Locale.US, "%.2f", latestBalance)} credit)"
                latestBalance < -0.01 -> "Owing $${String.format(java.util.Locale.US, "%.2f", -latestBalance)}"
                else -> "Settled ($0.00)"
            }

            MemberSummary(
                user = user,
                balance = latestBalance,
                totalApprovedSessions = approvedCount,
                pendingSessions = pendingCount,
                totalApprovedPaid = approvedPaid,
                totalAllocatedCost = totalAllocated,
                statusText = statusText
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Team Dashboard stats
    val teamDashboardStats: StateFlow<List<TeamDashboardStats>> = combine(
        allTeams,
        allAttendances,
        allBudgets,
        allPayments,
        memberSummaries
    ) { teams, attendances, budgets, payments, members ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)

        teams.map { team ->
            val teamAttendances = attendances.filter { it.teamId == team.id && it.status == "APPROVED" }
            val teamBudget = budgets.filter { it.teamId == team.id && it.periodMonth == currentMonth && it.periodYear == currentYear }.sumOf { it.totalAmount }
            val memberCount = members.size // overall or active in team

            val totalCollected = payments.filter { it.status == "APPROVED" }.sumOf { it.amount }
            val totalOwing = members.filter { it.balance < 0 }.sumOf { -it.balance }

            TeamDashboardStats(
                team = team,
                memberCount = memberCount,
                totalApprovedSessionsThisMonth = teamAttendances.size,
                totalBudgetValue = if (teamBudget > 0) teamBudget else team.monthlyBudgetGoal,
                totalCollected = totalCollected,
                totalOutstanding = totalOwing
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun submitAttendance(
        teamId: Long,
        sessionDate: Long,
        sessionType: String,
        notes: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.submitAttendance(
                Attendance(
                    userId = user.id,
                    teamId = teamId,
                    sessionDate = sessionDate,
                    sessionType = sessionType,
                    notes = notes,
                    status = "PENDING"
                )
            )
            _snackbarMessage.value = "Attendance record submitted for approval."
        }
    }

    fun approveAttendance(attendanceId: Long, reviewerNotes: String? = null) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.approveAttendance(attendanceId, admin.id, reviewerNotes)
            _snackbarMessage.value = "Attendance approved."
        }
    }

    fun rejectAttendance(attendanceId: Long, reason: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.rejectAttendance(attendanceId, admin.id, reason)
            _snackbarMessage.value = "Attendance rejected."
        }
    }

    fun bulkApproveAttendances() {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.bulkApproveAllPendingAttendances(admin.id)
            _snackbarMessage.value = "All pending attendance submissions approved."
        }
    }

    fun submitPayment(
        amount: Double,
        paymentDate: Long,
        method: String,
        referenceNote: String,
        receiptNote: String?
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.submitPayment(
                Payment(
                    userId = user.id,
                    amount = amount,
                    paymentDate = paymentDate,
                    paymentMethod = method,
                    referenceNote = referenceNote,
                    receiptNote = receiptNote,
                    status = "PENDING"
                )
            )
            _snackbarMessage.value = "Payment record submitted for admin verification."
        }
    }

    fun approvePayment(paymentId: Long, reviewerNotes: String? = null) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.approvePayment(paymentId, admin.id, reviewerNotes)
            _snackbarMessage.value = "Payment approved and credited to member ledger."
        }
    }

    fun rejectPayment(paymentId: Long, reason: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.rejectPayment(paymentId, admin.id, reason)
            _snackbarMessage.value = "Payment rejected."
        }
    }

    fun createTeamBudget(
        teamId: Long,
        title: String,
        description: String,
        amount: Double,
        periodMonth: Int,
        periodYear: Int,
        category: String = "COURT_RENTAL",
        attachmentUrl: String? = null,
        attachmentType: String? = "RECEIPT_IMAGE",
        attachmentName: String? = null
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createBudget(
                TeamBudget(
                    teamId = teamId,
                    title = title,
                    description = description,
                    totalAmount = amount,
                    periodMonth = periodMonth,
                    periodYear = periodYear,
                    category = category,
                    attachmentUrl = attachmentUrl,
                    attachmentType = attachmentType,
                    attachmentName = attachmentName,
                    declaredByUserId = admin.id,
                    status = "APPROVED"
                ),
                adminId = admin.id
            )
            _snackbarMessage.value = "Cost item & budget declared: $title ($${String.format(Locale.US, "%.2f", amount)})"
        }
    }

    fun generateInvoiceAndAllocate(budgetId: Long) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.generateInvoiceAndAllocate(budgetId, admin.id)
            if (result.isSuccess) {
                _snackbarMessage.value = "Invoice ${result.getOrNull()?.invoiceNumber} generated and costs allocated proportionally!"
            } else {
                _snackbarMessage.value = "Allocation failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun recalculateInvoice(invoiceId: Long) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.recalculateInvoice(invoiceId, admin.id)
            if (result.isSuccess) {
                _snackbarMessage.value = "Invoice recalculated and ledger updated!"
            } else {
                _snackbarMessage.value = "Recalculation failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun applyManualAdjustment(
        userId: Long,
        adjustmentType: String,
        amount: Double,
        reason: String
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.applyManualBalanceAdjustment(
                userId = userId,
                adjustmentType = adjustmentType,
                amount = amount,
                reason = reason,
                adminId = admin.id
            )
            _snackbarMessage.value = "Manual balance adjustment recorded in ledger."
        }
    }

    fun recordBatchOwnerAttendance(
        teamId: Long,
        sessionDate: Long,
        sessionType: String,
        notes: String,
        presentUserIds: List<Long>
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val count = repository.recordBatchOwnerAttendance(
                teamId = teamId,
                sessionDate = sessionDate,
                sessionType = sessionType,
                notes = notes,
                presentUserIds = presentUserIds,
                adminId = admin.id
            )
            _snackbarMessage.value = "Recorded and verified attendance for $count players."
        }
    }

    fun recordBatchAttendance(
        teamId: Long,
        sessionDate: Long,
        sessionType: String,
        notes: String,
        presentUserIds: List<Long>
    ) {
        recordBatchOwnerAttendance(teamId, sessionDate, sessionType, notes, presentUserIds)
    }

    fun joinTeam(userId: Long, teamId: Long, role: String = "MEMBER") {
        viewModelScope.launch {
            repository.joinTeam(userId, teamId, role)
            _snackbarMessage.value = "User assigned to team with role '$role'."
        }
    }

    fun issuePaymentRequest(
        userIds: List<Long>,
        title: String,
        amount: Double,
        memo: String
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val count = repository.issuePaymentRequest(
                userIds = userIds,
                title = title,
                amount = amount,
                memo = memo,
                adminId = admin.id
            )
            _snackbarMessage.value = "Issued payment request for $$amount to $count member(s)."
        }
    }

    fun registerNewMember(name: String, email: String, phone: String, teamId: Long?) {
        viewModelScope.launch {
            val colorList = listOf(0xFF00897B, 0xFFE65100, 0xFF6A1B9A, 0xFF2E7D32, 0xFFC2185B, 0xFF0D47A1)
            val newUserId = repository.createUser(
                User(
                    name = name,
                    email = email,
                    phone = phone,
                    role = "MEMBER",
                    avatarColorHex = colorList.random()
                )
            )
            if (teamId != null && teamId > 0) {
                repository.joinTeam(newUserId, teamId)
            }
            _snackbarMessage.value = "Member '$name' registered successfully."
        }
    }

    fun createNewTeam(
        name: String,
        sportType: String,
        description: String,
        monthlyBudget: Double,
        customInviteCode: String = ""
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val colorList = listOf(0xFF00897B, 0xFFE65100, 0xFF6A1B9A, 0xFF2E7D32, 0xFF0D47A1)
            val generatedCode = if (customInviteCode.isNotBlank()) {
                customInviteCode.trim().uppercase()
            } else {
                val cleanPrefix = name.uppercase().replace(Regex("[^A-Z0-9]"), "").take(6).ifBlank { "TEAM" }
                val randomSuffix = (1000..9999).random()
                "$cleanPrefix-$randomSuffix"
            }
            repository.createTeam(
                Team(
                    name = name,
                    sportType = sportType,
                    description = description,
                    monthlyBudgetGoal = monthlyBudget,
                    colorHex = colorList.random(),
                    inviteCode = generatedCode
                ),
                adminId = admin.id
            )
            _snackbarMessage.value = "Team '$name' created! Invite code: $generatedCode"
        }
    }

    // --- Join Team via Link / Invite Code Actions ---
    suspend fun previewTeamForCode(codeOrLink: String): Team? {
        return repository.findTeamByCode(codeOrLink)
    }

    fun submitJoinRequest(
        teamCodeOrLink: String,
        name: String,
        email: String,
        phone: String,
        message: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val current = _currentUser.value
        viewModelScope.launch {
            val result = repository.submitJoinRequest(
                teamCodeOrLink = teamCodeOrLink,
                applicantName = name,
                applicantEmail = email,
                applicantPhone = phone,
                message = message,
                existingUserId = current?.id
            )
            if (result.isSuccess) {
                _snackbarMessage.value = "Join request submitted! The team owner will review and approve."
                onResult(true, "Request sent! You will be notified once approved.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to submit join request."
                _snackbarMessage.value = errorMsg
                onResult(false, errorMsg)
            }
        }
    }

    fun approveJoinRequest(requestId: Long, notes: String? = null) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.approveJoinRequest(
                requestId = requestId,
                adminId = admin.id,
                notes = notes
            )
            if (result.isSuccess) {
                val user = result.getOrNull()
                _snackbarMessage.value = "Approved ${user?.name ?: "Member"}! Added to team roster."
            } else {
                _snackbarMessage.value = "Approval failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun rejectJoinRequest(requestId: Long, reason: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.rejectJoinRequest(
                requestId = requestId,
                adminId = admin.id,
                reason = reason
            )
            if (result.isSuccess) {
                _snackbarMessage.value = "Join request declined."
            } else {
                _snackbarMessage.value = "Action failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun getTeamInviteLink(team: Team): String {
        val code = team.inviteCode.ifBlank { "TEAM-${team.id}" }
        return "https://clubledger.app/join?team=$code"
    }

    // --- Member Dispute Actions ---
    fun raiseDispute(
        category: String,
        referenceType: String,
        referenceId: Long,
        title: String,
        description: String,
        requestedAdjustmentAmount: Double
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.raiseDispute(
                userId = user.id,
                category = category,
                referenceType = referenceType,
                referenceId = referenceId,
                title = title,
                description = description,
                requestedAdjustmentAmount = requestedAdjustmentAmount
            )
            _snackbarMessage.value = "Dispute submitted! The team treasurer will review your request."
        }
    }

    fun resolveDispute(
        disputeId: Long,
        resolutionAction: String,
        resolutionNotes: String,
        creditAmount: Double
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.resolveDispute(
                disputeId = disputeId,
                adminId = admin.id,
                resolutionAction = resolutionAction,
                resolutionNotes = resolutionNotes,
                creditAmount = creditAmount
            )
            _snackbarMessage.value = when (resolutionAction) {
                "CREDIT_AND_RESOLVE" -> "Dispute resolved with +$${String.format(java.util.Locale.US, "%.2f", creditAmount)} ledger credit adjustment."
                "EXPLAIN_AND_RESOLVE" -> "Dispute resolved with explanation."
                else -> "Dispute dismissed."
            }
        }
    }

    // Export helpers
    fun getAttendanceCsvString(): String {
        val usersMap = allUsers.value.associateBy { it.id }
        val teamsMap = allTeams.value.associateBy { it.id }
        return repository.generateAttendanceCsv(allAttendances.value, usersMap, teamsMap)
    }

    fun getPaymentsCsvString(): String {
        val usersMap = allUsers.value.associateBy { it.id }
        return repository.generatePaymentsCsv(allPayments.value, usersMap)
    }

    fun getLedgerCsvString(): String {
        val usersMap = allUsers.value.associateBy { it.id }
        return repository.generateLedgerCsv(allLedgerEntries.value, usersMap)
    }

    fun syncUserAccess(sourceUserId: Long, targetUserId: Long) {
        viewModelScope.launch {
            repository.syncUserAccess(sourceUserId, targetUserId)
            _snackbarMessage.value = "Permissions and team access synchronized successfully."
        }
    }

    fun updateUserRole(userId: Long, newRole: String) {
        viewModelScope.launch {
            repository.updateUserRole(userId, newRole)
            _snackbarMessage.value = "User role updated to $newRole."
        }
    }

    fun updateTeamMembershipRole(userId: Long, teamId: Long, newRole: String) {
        viewModelScope.launch {
            repository.updateTeamMembershipRole(userId, teamId, newRole)
            _snackbarMessage.value = "Team role updated to $newRole."
        }
    }

    fun hasTeamAdminAccess(teamId: Long): Boolean {
        return PermissionEngine.hasTeamAdminAccess(_currentUser.value, teamId, allMemberships.value)
    }

    fun hasTeamCaptainAccess(teamId: Long): Boolean {
        return PermissionEngine.hasTeamCaptainAccess(_currentUser.value, teamId, allMemberships.value)
    }

    fun getUserRoleBadge(user: User): String {
        val teamsMap = allTeams.value.associateBy { it.id }
        return PermissionEngine.formatMultiRoleBadge(user, allMemberships.value, teamsMap)
    }
}
