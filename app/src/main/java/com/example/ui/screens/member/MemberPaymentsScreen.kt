package com.example.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BalanceLedger
import com.example.data.entity.Dispute
import com.example.data.entity.Payment
import com.example.data.entity.User
import com.example.ui.components.RaiseDisputeDialog
import com.example.ui.components.StatusChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPaymentsScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    val currentBalance by viewModel.currentUserBalance.collectAsState()
    val payments by viewModel.currentUserPayments.collectAsState()
    val ledgerEntries by viewModel.currentUserLedger.collectAsState()
    val userDisputes by viewModel.currentUserDisputes.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Payment Records, 1 = Balance Ledger, 2 = My Disputes
    var showSubmitPaymentDialog by remember { mutableStateOf(false) }
    var selectedPaymentForDispute by remember { mutableStateOf<Payment?>(null) }
    var showGeneralDisputeDialog by remember { mutableStateOf(false) }

    val totalApprovedPaid = payments.filter { it.status == "APPROVED" }.sumOf { it.amount }
    val totalPendingPaid = payments.filter { it.status == "PENDING" }.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSubmitPaymentDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Submit Payment") },
                modifier = Modifier.testTag("submit_payment_fab")
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "My Payments & Running Balance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track payments, pending verifications, and your balance ledger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Running Balance Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CURRENT BALANCE", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                color = when {
                                    currentBalance > 0.01 -> SuccessContainer
                                    currentBalance < -0.01 -> ErrorContainer
                                    else -> PrimaryContainer
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        currentBalance > 0.01 -> "OVERPAID (CREDIT)"
                                        currentBalance < -0.01 -> "AMOUNT DUE"
                                        else -> "PAID IN FULL"
                                    },
                                    color = when {
                                        currentBalance > 0.01 -> SuccessGreen
                                        currentBalance < -0.01 -> ErrorRed
                                        else -> OnPrimaryContainer
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                currentBalance > 0.01 -> "+$${String.format(Locale.US, "%.2f", currentBalance)}"
                                currentBalance < -0.01 -> "$${String.format(Locale.US, "%.2f", -currentBalance)}"
                                else -> "$0.00"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Approved Paid: $${String.format(Locale.US, "%.2f", totalApprovedPaid)}", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodyMedium)
                            if (totalPendingPaid > 0) {
                                Text("Pending: $${String.format(Locale.US, "%.2f", totalPendingPaid)}", color = WarningAmber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tab Selector
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryGreen
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Payments (${payments.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Ledger (${ledgerEntries.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Disputes (${userDisputes.size})", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            if (selectedTab == 0) {
                // Payments List
                if (payments.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No payment records yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Submit a bank transfer, cash, or card payment receipt.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(payments) { payment ->
                        val existingDispute = userDisputes.firstOrNull { it.referenceType == "PAYMENT" && it.referenceId == payment.id }
                        PaymentItemCard(
                            payment = payment,
                            existingDispute = existingDispute,
                            onDisputeClick = { selectedPaymentForDispute = payment }
                        )
                    }
                }
            } else if (selectedTab == 1) {
                // Immutable Balance Ledger
                if (ledgerEntries.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("No ledger transactions recorded yet.", color = TextSecondary, modifier = Modifier.padding(20.dp))
                        }
                    }
                } else {
                    items(ledgerEntries) { entry ->
                        LedgerEntryCard(entry = entry)
                    }
                }
            } else {
                // My Disputes List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Submitted Disputes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { showGeneralDisputeDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Raise Inquiry", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (userDisputes.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Active Disputes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Your account is in good standing. If you notice any incorrect session fees, attendance omissions, or rejected payments, you can raise a dispute here.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(userDisputes) { dispute ->
                        MemberDisputeCard(dispute = dispute)
                    }
                }
            }
        }
    }

    if (showSubmitPaymentDialog) {
        SubmitPaymentDialog(
            suggestedAmount = if (currentBalance < 0) -currentBalance else 50.0,
            onDismiss = { showSubmitPaymentDialog = false },
            onSubmit = { amount, dateMillis, method, refNote, receiptNote ->
                viewModel.submitPayment(amount, dateMillis, method, refNote, receiptNote)
                showSubmitPaymentDialog = false
            }
        )
    }

    selectedPaymentForDispute?.let { payment ->
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        RaiseDisputeDialog(
            initialCategory = "PAYMENT_REJECTED",
            initialReferenceType = "PAYMENT",
            initialReferenceId = payment.id,
            initialTitle = "Dispute on Payment of $${String.format(Locale.US, "%.2f", payment.amount)} (${sdf.format(Date(payment.paymentDate))})",
            initialRequestedAmount = payment.amount,
            contextSummaryText = "${payment.paymentMethod.replace("_", " ")} Payment of $${String.format(Locale.US, "%.2f", payment.amount)} - Status: ${payment.status}",
            onDismiss = { selectedPaymentForDispute = null },
            onSubmit = { category, refType, refId, title, desc, reqAmt ->
                viewModel.raiseDispute(
                    category = category,
                    referenceType = refType,
                    referenceId = refId,
                    title = title,
                    description = desc,
                    requestedAdjustmentAmount = reqAmt
                )
                selectedPaymentForDispute = null
            }
        )
    }

    if (showGeneralDisputeDialog) {
        RaiseDisputeDialog(
            initialCategory = "GENERAL",
            initialReferenceType = "OTHER",
            initialReferenceId = 0,
            initialTitle = "Treasurer Balance / Billing Review Request",
            initialRequestedAmount = 0.0,
            contextSummaryText = "General Review for Member: ${currentUser.name}",
            onDismiss = { showGeneralDisputeDialog = false },
            onSubmit = { category, refType, refId, title, desc, reqAmt ->
                viewModel.raiseDispute(
                    category = category,
                    referenceType = refType,
                    referenceId = refId,
                    title = title,
                    description = desc,
                    requestedAdjustmentAmount = reqAmt
                )
                showGeneralDisputeDialog = false
            }
        )
    }
}

@Composable
fun PaymentItemCard(
    payment: Payment,
    existingDispute: Dispute? = null,
    onDisputeClick: () -> Unit = {}
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (payment.status == "APPROVED") SuccessContainer else WarningContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (payment.paymentMethod) {
                                "BANK_TRANSFER" -> Icons.Default.AccountBalance
                                "CASH" -> Icons.Default.AttachMoney
                                "VENMO_ZELLE" -> Icons.Default.SendToMobile
                                "CARD" -> Icons.Default.CreditCard
                                else -> Icons.Default.Payment
                            },
                            contentDescription = null,
                            tint = if (payment.status == "APPROVED") SuccessGreen else WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", payment.amount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${payment.paymentMethod.replace("_", " ")} • ${sdf.format(Date(payment.paymentDate))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                StatusChip(status = payment.status)
            }

            if (payment.referenceNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ref / Memo: ${payment.referenceNote}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            if (!payment.receiptNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Receipt: ${payment.receiptNote}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (!payment.reviewNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (payment.status == "APPROVED") SuccessContainer.copy(alpha = 0.5f) else ErrorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (payment.status == "APPROVED") Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (payment.status == "APPROVED") SuccessGreen else ErrorRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admin Note: ${payment.reviewNotes}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (payment.status == "APPROVED") SuccessGreen else ErrorRed
                        )
                    }
                }
            }

            if (existingDispute != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (existingDispute.status == "OPEN") WarningContainer.copy(alpha = 0.5f) else SuccessContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (existingDispute.status == "OPEN") Icons.Default.Schedule else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (existingDispute.status == "OPEN") WarningAmber else SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dispute: ${existingDispute.status} - \"${existingDispute.title}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (payment.status == "REJECTED" || payment.status == "PENDING") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onDisputeClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispute / Review", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberDisputeCard(dispute: Dispute) {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dispute.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Category: ${dispute.category.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StatusChip(status = dispute.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dispute.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            if (dispute.requestedAdjustmentAmount > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Requested Credit: $${String.format(Locale.US, "%.2f", dispute.requestedAdjustmentAmount)}",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Submitted: ${sdf.format(Date(dispute.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (!dispute.resolutionNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = if (dispute.status.startsWith("RESOLVED")) SuccessContainer.copy(alpha = 0.5f) else ErrorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Treasurer Resolution:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dispute.status.startsWith("RESOLVED")) SuccessGreen else ErrorRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dispute.resolutionNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerEntryCard(entry: BalanceLedger) {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)
    val isCredit = entry.amount > 0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCredit) SuccessContainer else ErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isCredit) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${entry.type.replace("_", " ")} • ${sdf.format(Date(entry.createdAt))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else ""}$${String.format(Locale.US, "%.2f", entry.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) SuccessGreen else ErrorRed
                )
                Text(
                    text = "Bal: $${String.format(Locale.US, "%.2f", entry.runningBalanceAfter)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun SubmitPaymentDialog(
    suggestedAmount: Double,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, dateMillis: Long, method: String, refNote: String, receiptNote: String?) -> Unit
) {
    var amountText by remember { mutableStateOf(if (suggestedAmount > 0) String.format(Locale.US, "%.2f", suggestedAmount) else "50.00") }
    var selectedMethod by remember { mutableStateOf("BANK_TRANSFER") }
    var referenceNote by remember { mutableStateOf("") }
    var receiptNote by remember { mutableStateOf("") }

    val methods = listOf(
        "BANK_TRANSFER" to "Bank Transfer",
        "VENMO_ZELLE" to "Venmo / Zelle",
        "CASH" to "Cash in hand",
        "CARD" to "Debit / Credit Card",
        "CHECK" to "Check"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Payment Record", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Payment Method:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Column {
                    methods.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedMethod == key) PrimaryContainer else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedMethod == key,
                                onClick = { selectedMethod = key }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = referenceNote,
                    onValueChange = { referenceNote = it },
                    label = { Text("Transaction Reference / Memo") },
                    placeholder = { Text("e.g. Wire Ref #TX-8291 or Cash to Coach") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_reference_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = receiptNote,
                    onValueChange = { receiptNote = it },
                    label = { Text("Receipt Details (optional)") },
                    placeholder = { Text("e.g. Screenshot confirmed via email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onSubmit(amount, System.currentTimeMillis(), selectedMethod, referenceNote, receiptNote)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("submit_payment_confirm_btn")
            ) {
                Text("Submit Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
