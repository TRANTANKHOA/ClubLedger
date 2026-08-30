package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String = "",
    val role: String = "MEMBER", // "ADMIN" or "MEMBER"
    val avatarColorHex: Long = 0xFF1E88E5,
    val joinedDate: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE" // "ACTIVE", "INACTIVE"
)

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sportType: String, // "Soccer", "Basketball", "Volleyball", "Running", "Tennis"
    val description: String = "",
    val colorHex: Long = 0xFF00897B,
    val monthlyBudgetGoal: Double = 1500.0,
    val inviteCode: String = "" // e.g. "RIVERSIDE-26"
)

@Entity(tableName = "team_memberships")
data class TeamMembership(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val teamId: Long,
    val roleInTeam: String = "MEMBER", // "MEMBER", "CAPTAIN", "COACH"
    val joinedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendances")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val teamId: Long,
    val sessionDate: Long, // timestamp in millis
    val sessionType: String, // "TRAINING", "MATCH", "TOURNAMENT", "FRIENDLY", "SOCIAL"
    val notes: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedByUserId: Long? = null,
    val reviewNotes: String? = null
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val amount: Double,
    val paymentDate: Long,
    val paymentMethod: String, // "BANK_TRANSFER", "CASH", "CARD", "VENMO_ZELLE", "CHECK", "OTHER"
    val referenceNote: String = "",
    val receiptNote: String? = null,
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedByUserId: Long? = null,
    val reviewNotes: String? = null
)

@Entity(tableName = "team_budgets")
data class TeamBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamId: Long,
    val periodMonth: Int, // 1 - 12
    val periodYear: Int, // e.g. 2026
    val totalAmount: Double,
    val title: String,
    val description: String = "",
    val category: String = "COURT_RENTAL", // "COURT_RENTAL", "TOURNAMENT", "EQUIPMENT", "COACHING_REFS", "TRANSPORT", "OTHER"
    val attachmentUrl: String? = null,
    val attachmentType: String? = "RECEIPT_IMAGE", // "RECEIPT_IMAGE", "PDF_INVOICE", "EXTERNAL_URL"
    val attachmentName: String? = null,
    val declaredByUserId: Long? = null,
    val status: String = "APPROVED", // "DRAFT", "APPROVED", "INVOICED", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val teamId: Long,
    val invoiceNumber: String,
    val title: String,
    val totalAmount: Double,
    val periodMonth: Int,
    val periodYear: Int,
    val category: String = "COURT_RENTAL",
    val attachmentUrl: String? = null,
    val attachmentType: String? = "RECEIPT_IMAGE",
    val attachmentName: String? = null,
    val status: String = "ISSUED", // "DRAFT", "ISSUED", "PAID_OUT", "CANCELLED"
    val issuedAt: Long = System.currentTimeMillis(),
    val totalApprovedSessions: Int = 0,
    val costPerSession: Double = 0.0
)

@Entity(tableName = "invoice_allocations")
data class InvoiceAllocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val userId: Long,
    val teamId: Long,
    val approvedSessionsCount: Int,
    val percentage: Double, // e.g. 15.5 (%)
    val allocatedAmount: Double, // e.g. 250.00
    val status: String = "APPLIED_TO_LEDGER", // "PENDING", "APPLIED_TO_LEDGER", "RECALCULATED", "CANCELLED"
    val calculatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "balance_ledger")
data class BalanceLedger(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val type: String, // "PAYMENT_CREDIT", "INVOICE_DEBIT", "MANUAL_ADJUSTMENT", "REFUND", "PENALTY"
    val amount: Double, // Positive for credit, Negative for debit
    val runningBalanceAfter: Double,
    val referenceType: String, // "PAYMENT", "INVOICE_ALLOCATION", "ADMIN_ADJUSTMENT"
    val referenceId: Long = 0,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdByUserId: Long? = null
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String, // "ATTENDANCE_APPROVED", "PAYMENT_APPROVED", "BUDGET_ALLOCATED", "MANUAL_ADJUSTMENT", etc.
    val entityType: String,
    val entityId: Long,
    val performedByUserId: Long,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "disputes")
data class Dispute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val category: String, // "ATTENDANCE_DISCREPANCY", "PAYMENT_REJECTED", "INVOICE_OVERCHARGE", "DUES_REQUEST", "GENERAL"
    val referenceType: String, // "ATTENDANCE", "PAYMENT", "INVOICE_ALLOCATION", "LEDGER_ENTRY", "OTHER"
    val referenceId: Long = 0,
    val title: String,
    val description: String,
    val requestedAdjustmentAmount: Double = 0.0,
    val status: String = "OPEN", // "OPEN", "RESOLVED_CREDITED", "RESOLVED_EXPLAINED", "DISMISSED"
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolvedByUserId: Long? = null,
    val resolutionNotes: String? = null
)

@Entity(tableName = "team_join_requests")
data class TeamJoinRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamId: Long,
    val applicantName: String,
    val applicantEmail: String,
    val applicantPhone: String = "",
    val message: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedByUserId: Long? = null,
    val reviewNotes: String? = null,
    val existingUserId: Long? = null
)

