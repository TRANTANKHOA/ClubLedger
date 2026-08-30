package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ClubDao
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        User::class,
        Team::class,
        TeamMembership::class,
        Attendance::class,
        Payment::class,
        TeamBudget::class,
        Invoice::class,
        InvoiceAllocation::class,
        BalanceLedger::class,
        AuditLog::class,
        Dispute::class,
        TeamJoinRequest::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ClubDatabase : RoomDatabase() {
    abstract fun clubDao(): ClubDao

    companion object {
        @Volatile
        private var INSTANCE: ClubDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ClubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClubDatabase::class.java,
                    "club_treasury_db"
                )
                    .addCallback(ClubDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class ClubDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.clubDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: ClubDao) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12

            // 1. Users
            val adminUser = User(
                id = 1,
                name = "Dave Miller",
                email = "dave.admin@sportsclub.org",
                phone = "+1 (555) 019-2831",
                role = "ADMIN",
                avatarColorHex = 0xFF0D47A1,
                joinedDate = now - 180L * 86400000L
            )
            val member1 = User(
                id = 2,
                name = "Alex Chen",
                email = "alex.chen@gmail.com",
                phone = "+1 (555) 014-9923",
                role = "MEMBER",
                avatarColorHex = 0xFF00897B,
                joinedDate = now - 120L * 86400000L
            )
            val member2 = User(
                id = 3,
                name = "Sarah Jenkins",
                email = "sarah.j@outlook.com",
                phone = "+1 (555) 017-8834",
                role = "MEMBER",
                avatarColorHex = 0xFFE65100,
                joinedDate = now - 90L * 86400000L
            )
            val member3 = User(
                id = 4,
                name = "Marcus Rodriguez",
                email = "marcus.rod@gmail.com",
                phone = "+1 (555) 012-7719",
                role = "MEMBER",
                avatarColorHex = 0xFF6A1B9A,
                joinedDate = now - 60L * 86400000L
            )
            val member4 = User(
                id = 5,
                name = "Emily Watson",
                email = "emily.watson@yahoo.com",
                phone = "+1 (555) 018-3341",
                role = "MEMBER",
                avatarColorHex = 0xFF2E7D32,
                joinedDate = now - 45L * 86400000L
            )
            val member5 = User(
                id = 6,
                name = "Liam Patel",
                email = "liam.patel@gmail.com",
                phone = "+1 (555) 016-5522",
                role = "MEMBER",
                avatarColorHex = 0xFFC2185B,
                joinedDate = now - 30L * 86400000L
            )

            dao.insertUser(adminUser)
            dao.insertUser(member1)
            dao.insertUser(member2)
            dao.insertUser(member3)
            dao.insertUser(member4)
            dao.insertUser(member5)

            // 2. Teams
            val teamSoccer = Team(
                id = 1,
                name = "Riverside FC First XI",
                sportType = "Soccer",
                description = "Competitive 11v11 weekend football league with mid-week tactical training.",
                colorHex = 0xFF00897B,
                monthlyBudgetGoal = 1800.0,
                inviteCode = "RIVERSIDE-26"
            )
            val teamBasket = Team(
                id = 2,
                name = "Metro Lions Basketball",
                sportType = "Basketball",
                description = "Indoor 5v5 basketball league and weekly open scrimmage sessions.",
                colorHex = 0xFFE65100,
                monthlyBudgetGoal = 1200.0,
                inviteCode = "METRO-HOOPS-26"
            )
            val teamVolley = Team(
                id = 3,
                name = "Community Volleyball",
                sportType = "Volleyball",
                description = "Mixed co-ed indoor & beach volleyball tournaments.",
                colorHex = 0xFF6A1B9A,
                monthlyBudgetGoal = 800.0,
                inviteCode = "BEACH-VOLLEY-26"
            )

            dao.insertTeam(teamSoccer)
            dao.insertTeam(teamBasket)
            dao.insertTeam(teamVolley)

            // 3. Team Memberships
            listOf(
                TeamMembership(1, 2, 1, "CAPTAIN", now - 120L * 86400000L),
                TeamMembership(2, 3, 1, "MEMBER", now - 90L * 86400000L),
                TeamMembership(3, 4, 1, "MEMBER", now - 60L * 86400000L),
                TeamMembership(4, 5, 1, "MEMBER", now - 45L * 86400000L),
                TeamMembership(5, 6, 1, "MEMBER", now - 30L * 86400000L),
                // Basketball team memberships
                TeamMembership(6, 2, 2, "MEMBER", now - 60L * 86400000L),
                TeamMembership(7, 3, 2, "CAPTAIN", now - 80L * 86400000L),
                TeamMembership(8, 4, 2, "MEMBER", now - 40L * 86400000L),
                // Volleyball
                TeamMembership(9, 5, 3, "CAPTAIN", now - 40L * 86400000L),
                TeamMembership(10, 6, 3, "MEMBER", now - 30L * 86400000L)
            ).forEach { dao.insertMembership(it) }

            // 4. Historical & Current Attendance (Soccer Team)
            val oneDay = 86400000L
            // Alex Chen (10 sessions approved, 1 pending)
            for (i in 1..10) {
                dao.insertAttendance(
                    Attendance(
                        userId = 2,
                        teamId = 1,
                        sessionDate = now - (i * 2L + 3) * oneDay,
                        sessionType = if (i % 3 == 0) "MATCH" else "TRAINING",
                        notes = "Attended full session #${11 - i}",
                        status = "APPROVED",
                        submittedAt = now - (i * 2L + 3) * oneDay,
                        reviewedAt = now - (i * 2L + 2) * oneDay,
                        reviewedByUserId = 1,
                        reviewNotes = "Verified by Coach"
                    )
                )
            }
            // Pending attendance for Alex
            dao.insertAttendance(
                Attendance(
                    userId = 2,
                    teamId = 1,
                    sessionDate = now - 1 * oneDay,
                    sessionType = "TRAINING",
                    notes = "Completed speed & agility drill session",
                    status = "PENDING",
                    submittedAt = now - 1 * oneDay
                )
            )

            // Sarah Jenkins (8 sessions approved, 1 pending)
            for (i in 1..8) {
                dao.insertAttendance(
                    Attendance(
                        userId = 3,
                        teamId = 1,
                        sessionDate = now - (i * 3L + 2) * oneDay,
                        sessionType = if (i % 2 == 0) "MATCH" else "TRAINING",
                        notes = "Midfield training drill",
                        status = "APPROVED",
                        submittedAt = now - (i * 3L + 2) * oneDay,
                        reviewedAt = now - (i * 3L + 1) * oneDay,
                        reviewedByUserId = 1,
                        reviewNotes = "Verified by Coach"
                    )
                )
            }
            dao.insertAttendance(
                Attendance(
                    userId = 3,
                    teamId = 1,
                    sessionDate = now - 1 * oneDay,
                    sessionType = "TRAINING",
                    notes = "Regular Thursday practice",
                    status = "PENDING",
                    submittedAt = now - 1 * oneDay
                )
            )

            // Marcus Rodriguez (6 sessions approved)
            for (i in 1..6) {
                dao.insertAttendance(
                    Attendance(
                        userId = 4,
                        teamId = 1,
                        sessionDate = now - (i * 4L + 2) * oneDay,
                        sessionType = "TRAINING",
                        notes = "Goalkeeping drills",
                        status = "APPROVED",
                        submittedAt = now - (i * 4L + 2) * oneDay,
                        reviewedAt = now - (i * 4L + 1) * oneDay,
                        reviewedByUserId = 1,
                        reviewNotes = "Verified by Coach"
                    )
                )
            }

            // Emily Watson (4 sessions approved, 1 rejected)
            for (i in 1..4) {
                dao.insertAttendance(
                    Attendance(
                        userId = 5,
                        teamId = 1,
                        sessionDate = now - (i * 5L + 1) * oneDay,
                        sessionType = "TRAINING",
                        notes = "Defensive drills",
                        status = "APPROVED",
                        submittedAt = now - (i * 5L + 1) * oneDay,
                        reviewedAt = now - (i * 5L) * oneDay,
                        reviewedByUserId = 1,
                        reviewNotes = "Verified by Coach"
                    )
                )
            }
            dao.insertAttendance(
                Attendance(
                    userId = 5,
                    teamId = 1,
                    sessionDate = now - 12 * oneDay,
                    sessionType = "MATCH",
                    notes = "Late arrival",
                    status = "REJECTED",
                    submittedAt = now - 12 * oneDay,
                    reviewedAt = now - 11 * oneDay,
                    reviewedByUserId = 1,
                    reviewNotes = "Did not check in with team sheet on match day"
                )
            )

            // Liam Patel (2 sessions approved, 1 pending)
            for (i in 1..2) {
                dao.insertAttendance(
                    Attendance(
                        userId = 6,
                        teamId = 1,
                        sessionDate = now - (i * 7L + 2) * oneDay,
                        sessionType = "TRAINING",
                        notes = "Conditioning session",
                        status = "APPROVED",
                        submittedAt = now - (i * 7L + 2) * oneDay,
                        reviewedAt = now - (i * 7L + 1) * oneDay,
                        reviewedByUserId = 1,
                        reviewNotes = "Verified"
                    )
                )
            }
            dao.insertAttendance(
                Attendance(
                    userId = 6,
                    teamId = 1,
                    sessionDate = now - 1 * oneDay,
                    sessionType = "TRAINING",
                    notes = "Thursday team practice",
                    status = "PENDING",
                    submittedAt = now - 1 * oneDay
                )
            )

            // 5. Team Budget & Invoice (Soccer Team - Total 30 sessions = 10 + 8 + 6 + 4 + 2)
            // Total budget $1,500. Cost per session = $1,500 / 30 = $50.00
            val budget1 = TeamBudget(
                id = 1,
                teamId = 1,
                periodMonth = currentMonth,
                periodYear = currentYear,
                totalAmount = 1500.0,
                title = "Facility Pitch Rental & Referee Fees",
                description = "Pitch rental (4 weeks) + Match referee stipends + Match balls",
                status = "INVOICED",
                createdAt = now - 20 * oneDay
            )
            dao.insertBudget(budget1)

            val invoice1 = Invoice(
                id = 1,
                budgetId = 1,
                teamId = 1,
                invoiceNumber = "INV-$currentYear-$currentMonth-RFC01",
                title = "Monthly Allocation - Riverside FC",
                totalAmount = 1500.0,
                periodMonth = currentMonth,
                periodYear = currentYear,
                status = "ISSUED",
                issuedAt = now - 15 * oneDay,
                totalApprovedSessions = 30,
                costPerSession = 50.0
            )
            dao.insertInvoice(invoice1)

            // Proportional allocations:
            // Alex: 10/30 = 33.33% = $500.00
            // Sarah: 8/30 = 26.67% = $400.00
            // Marcus: 6/30 = 20.00% = $300.00
            // Emily: 4/30 = 13.33% = $200.00
            // Liam: 2/30 = 6.67% = $100.00
            val allocations = listOf(
                InvoiceAllocation(1, 1, 2, 1, 10, 33.33, 500.0, "APPLIED_TO_LEDGER", now - 15 * oneDay),
                InvoiceAllocation(2, 1, 3, 1, 8, 26.67, 400.0, "APPLIED_TO_LEDGER", now - 15 * oneDay),
                InvoiceAllocation(3, 1, 4, 1, 6, 20.00, 300.0, "APPLIED_TO_LEDGER", now - 15 * oneDay),
                InvoiceAllocation(4, 1, 5, 1, 4, 13.33, 200.0, "APPLIED_TO_LEDGER", now - 15 * oneDay),
                InvoiceAllocation(5, 1, 6, 1, 2, 6.67, 100.0, "APPLIED_TO_LEDGER", now - 15 * oneDay)
            )
            dao.insertAllocations(allocations)

            // 6. Payments & Ledger setup
            // Alex Chen: Paid $500 (Fully Paid, Balance = $0)
            val pAlex = Payment(
                id = 1,
                userId = 2,
                amount = 500.0,
                paymentDate = now - 10 * oneDay,
                paymentMethod = "BANK_TRANSFER",
                referenceNote = "Ref #TX-984210 - March Fee",
                receiptNote = "Bank confirmation recvd",
                status = "APPROVED",
                submittedAt = now - 10 * oneDay,
                reviewedAt = now - 9 * oneDay,
                reviewedByUserId = 1,
                reviewNotes = "Payment cleared in club account"
            )
            dao.insertPayment(pAlex)

            // Sarah Jenkins: Paid $450 (Overpaid / Credit +$50, or paid 450 against 400 = +50 credit)
            val pSarah = Payment(
                id = 2,
                userId = 3,
                amount = 450.0,
                paymentDate = now - 8 * oneDay,
                paymentMethod = "VENMO_ZELLE",
                referenceNote = "Zelle - Sarah J (Extra for jersey)",
                receiptNote = "Zelle Conf #Z90184",
                status = "APPROVED",
                submittedAt = now - 8 * oneDay,
                reviewedAt = now - 7 * oneDay,
                reviewedByUserId = 1,
                reviewNotes = "Approved with credit"
            )
            dao.insertPayment(pSarah)

            // Marcus Rodriguez: Paid $150 (Partial payment, owes $150 remaining)
            val pMarcus = Payment(
                id = 3,
                userId = 4,
                amount = 150.0,
                paymentDate = now - 6 * oneDay,
                paymentMethod = "CASH",
                referenceNote = "Cash handed to coach at practice",
                receiptNote = "Receipt #C-042",
                status = "APPROVED",
                submittedAt = now - 6 * oneDay,
                reviewedAt = now - 5 * oneDay,
                reviewedByUserId = 1,
                reviewNotes = "Cash received and recorded in ledger"
            )
            dao.insertPayment(pMarcus)

            // Emily Watson: Pending payment $200 (Has not been approved yet, owes $200)
            val pEmily = Payment(
                id = 4,
                userId = 5,
                amount = 200.0,
                paymentDate = now - 2 * oneDay,
                paymentMethod = "BANK_TRANSFER",
                referenceNote = "Bank Ref #BT-7740",
                receiptNote = "Transferred on mobile banking",
                status = "PENDING",
                submittedAt = now - 2 * oneDay
            )
            dao.insertPayment(pEmily)

            // Liam Patel: Pending payment $100
            val pLiam = Payment(
                id = 5,
                userId = 6,
                amount = 100.0,
                paymentDate = now - 1 * oneDay,
                paymentMethod = "VENMO_ZELLE",
                referenceNote = "Venmo @liam-patel",
                receiptNote = "Sent via Venmo",
                status = "PENDING",
                submittedAt = now - 1 * oneDay
            )
            dao.insertPayment(pLiam)

            // Write initial BalanceLedger records
            // Alex: Debit -$500, Credit +$500 -> Running Balance: $0.00
            dao.insertLedgerEntry(
                BalanceLedger(
                    userId = 2,
                    type = "INVOICE_DEBIT",
                    amount = -500.0,
                    runningBalanceAfter = -500.0,
                    referenceType = "INVOICE_ALLOCATION",
                    referenceId = 1,
                    description = "Allocated cost for INV-$currentYear-$currentMonth-RFC01 (10 sessions)",
                    createdAt = now - 15 * oneDay,
                    createdByUserId = 1
                )
            )
            dao.insertLedgerEntry(
                BalanceLedger(
                    userId = 2,
                    type = "PAYMENT_CREDIT",
                    amount = 500.0,
                    runningBalanceAfter = 0.0,
                    referenceType = "PAYMENT",
                    referenceId = 1,
                    description = "Payment received (BANK_TRANSFER) - Ref #TX-984210",
                    createdAt = now - 9 * oneDay,
                    createdByUserId = 1
                )
            )

            // Sarah: Debit -$400, Credit +$450 -> Running Balance: +$50.00
            dao.insertLedgerEntry(
                BalanceLedger(
                    userId = 3,
                    type = "INVOICE_DEBIT",
                    amount = -400.0,
                    runningBalanceAfter = -400.0,
                    referenceType = "INVOICE_ALLOCATION",
                    referenceId = 2,
                    description = "Allocated cost for INV-$currentYear-$currentMonth-RFC01 (8 sessions)",
                    createdAt = now - 15 * oneDay,
                    createdByUserId = 1
                )
            )
            dao.insertLedgerEntry(
                BalanceLedger(
                    userId = 3,
                    type = "PAYMENT_CREDIT",
                    amount = 450.0,
                    runningBalanceAfter = 50.0,
                    referenceType = "PAYMENT",
                    referenceId = 2,
                    description = "Payment received (VENMO_ZELLE) - Zelle - Sarah J",
                    createdAt = now - 7 * oneDay,
                    createdByUserId = 1
                )
            )

            // Marcus: Debit -$300, Credit +$150 -> Running Balance: -$150.00 (Due $150)
            dao.insertLedgerEntry(
                BalanceLedger(
                    userId = 4,
                    type = "INVOICE_DEBIT",
                    amount = -300.0,
                    runningBalanceAfter = -300.0,
                    referenceType = "INVOICE_ALLOCATION",
                    referenceId = 3,
                    description = "Allocated cost for INV-$currentYear-$currentMonth-RFC01 (6 sessions)",
                    createdAt = now - 15 * oneDay,
                    createdByUserId = 1
                )
            )
            dao.insertLedgerEntry(
                BalanceLedger(
                    userId = 4,
                    type = "PAYMENT_CREDIT",
                    amount = 150.0,
                    runningBalanceAfter = -150.0,
                    referenceType = "PAYMENT",
                    referenceId = 3,
                    description = "Partial payment (CASH) - Receipt #C-042",
                    createdAt = now - 5 * oneDay,
                    createdByUserId = 1
                )
            )

            // Emily: Debit -$200 -> Running Balance: -$200.00 (Pending Payment not in ledger yet!)
            dao.insertLedgerEntry(
                BalanceLedger(
                    userId = 5,
                    type = "INVOICE_DEBIT",
                    amount = -200.0,
                    runningBalanceAfter = -200.0,
                    referenceType = "INVOICE_ALLOCATION",
                    referenceId = 4,
                    description = "Allocated cost for INV-$currentYear-$currentMonth-RFC01 (4 sessions)",
                    createdAt = now - 15 * oneDay,
                    createdByUserId = 1
                )
            )

            // Liam: Debit -$100 -> Running Balance: -$100.00 (Pending Payment not in ledger yet!)
            dao.insertLedgerEntry(
                BalanceLedger(
                    userId = 6,
                    type = "INVOICE_DEBIT",
                    amount = -100.0,
                    runningBalanceAfter = -100.0,
                    referenceType = "INVOICE_ALLOCATION",
                    referenceId = 5,
                    description = "Allocated cost for INV-$currentYear-$currentMonth-RFC01 (2 sessions)",
                    createdAt = now - 15 * oneDay,
                    createdByUserId = 1
                )
            )

            // Audit Logs
            dao.insertAuditLog(
                AuditLog(
                    action = "BUDGET_ALLOCATED",
                    entityType = "Invoice",
                    entityId = 1,
                    performedByUserId = 1,
                    details = "Generated Invoice INV-$currentYear-$currentMonth-RFC01 for $1,500.00 across 30 approved sessions.",
                    timestamp = now - 15 * oneDay
                )
            )
            dao.insertAuditLog(
                AuditLog(
                    action = "PAYMENT_APPROVED",
                    entityType = "Payment",
                    entityId = 1,
                    performedByUserId = 1,
                    details = "Approved $500.00 payment from Alex Chen via Bank Transfer.",
                    timestamp = now - 9 * oneDay
                )
            )
            dao.insertAuditLog(
                AuditLog(
                    action = "PAYMENT_APPROVED",
                    entityType = "Payment",
                    entityId = 2,
                    performedByUserId = 1,
                    details = "Approved $450.00 payment from Sarah Jenkins via Venmo/Zelle (includes +$50.00 credit).",
                    timestamp = now - 7 * oneDay
                )
            )
            dao.insertAuditLog(
                AuditLog(
                    action = "PAYMENT_APPROVED",
                    entityType = "Payment",
                    entityId = 3,
                    performedByUserId = 1,
                    details = "Approved partial cash payment of $150.00 from Marcus Rodriguez.",
                    timestamp = now - 5 * oneDay
                )
            )

            // Initial Sample Disputes
            dao.insertDispute(
                Dispute(
                    id = 1,
                    userId = 4, // Emily Taylor
                    category = "ATTENDANCE_DISCREPANCY",
                    referenceType = "ATTENDANCE",
                    referenceId = 0,
                    title = "Omitted from July 28 Friendly Match Roster",
                    description = "I played the full 90 minutes in the friendly vs West Coast Rovers on July 28th, but my attendance record was marked absent. Requesting 1 attendance credit.",
                    requestedAdjustmentAmount = 0.0,
                    status = "OPEN",
                    createdAt = now - 3 * oneDay
                )
            )

            // Initial Sample Join Requests from Join Link
            dao.insertJoinRequest(
                TeamJoinRequest(
                    id = 1,
                    teamId = 1,
                    applicantName = "Jordan Vance",
                    applicantEmail = "jordan.vance@gmail.com",
                    applicantPhone = "+1 (555) 018-9944",
                    message = "Hi Coach! Joined via the team invite link. I play central attacking midfielder and moved to the area recently.",
                    status = "PENDING",
                    createdAt = now - 1 * oneDay
                )
            )
            dao.insertJoinRequest(
                TeamJoinRequest(
                    id = 2,
                    teamId = 2,
                    applicantName = "Carlos Mendez",
                    applicantEmail = "carlos.mendez@outlook.com",
                    applicantPhone = "+1 (555) 011-3321",
                    message = "Interested in the weekly basketball scrimmages and tournament team.",
                    status = "PENDING",
                    createdAt = now - 12 * 3600000L
                )
            )
        }
    }
}
