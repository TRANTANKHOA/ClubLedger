package com.example.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.data.entity.InvoiceAllocation
import com.example.data.entity.User
import com.example.ui.components.CostCalculationDialog
import com.example.ui.components.RaiseDisputeDialog
import com.example.ui.components.StatusChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberInvoicesScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    val allocations by viewModel.currentUserAllocations.collectAsState()
    val invoices by viewModel.allInvoices.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val teams by viewModel.allTeams.collectAsState()
    val userDisputes by viewModel.currentUserDisputes.collectAsState()

    var selectedAllocationForExplanation by remember { mutableStateOf<InvoiceAllocation?>(null) }
    var selectedAllocationForDispute by remember { mutableStateOf<InvoiceAllocation?>(null) }

    val totalAllocated = allocations.sumOf { it.allocatedAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "My Invoices & Cost Allocations",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fair, attendance-based team budget distribution.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Total Invoiced Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TOTAL ALLOCATED CHARGES TO DATE",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", totalAllocated)}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Calculated from ${allocations.sumOf { it.approvedSessionsCount }} total approved sessions across ${allocations.size} invoice cycles.",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // How cost allocation works info box
        item {
            Surface(
                color = PrimaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Fair Cost Allocation Rule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimaryContainer
                        )
                        Text(
                            text = "Members with zero attendance owe $0. If you join mid-month, you only share costs for sessions after joining.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryDark
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Allocated Invoices (${allocations.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (allocations.isEmpty()) {
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
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No invoice allocations yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("When the admin issues a monthly team budget invoice, your proportional share will appear here.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        } else {
            items(allocations) { alloc ->
                val invoice = invoices.firstOrNull { it.id == alloc.invoiceId }
                val team = teams.firstOrNull { it.id == alloc.teamId }
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAllocationForExplanation = alloc }
                        .testTag("invoice_allocation_card_${alloc.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = invoice?.title ?: "Team Budget Invoice",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Invoice #${invoice?.invoiceNumber ?: "INV-${alloc.invoiceId}"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            StatusChip(status = alloc.status)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid
                        Surface(
                            color = SurfaceLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Team", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Text(team?.name ?: "Team", fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("My Sessions", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Text("${alloc.approvedSessionsCount} / ${invoice?.totalApprovedSessions ?: 0}", fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Team Share", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Text("${alloc.percentage}%", fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Check if this allocation has an open or resolved dispute
                        val existingDispute = userDisputes.firstOrNull { it.referenceType == "INVOICE_ALLOCATION" && it.referenceId == alloc.id }

                        if (existingDispute != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (existingDispute.status == "OPEN") WarningContainer.copy(alpha = 0.5f) else SuccessContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (existingDispute.status == "OPEN") Icons.Default.Schedule else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (existingDispute.status == "OPEN") WarningAmber else SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Dispute #${existingDispute.id}: ${existingDispute.status} - \"${existingDispute.title}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Allocated Cost", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", alloc.allocatedAmount)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (existingDispute == null) {
                                    OutlinedButton(
                                        onClick = { selectedAllocationForDispute = alloc },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Dispute", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Button(
                                    onClick = { selectedAllocationForExplanation = alloc },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Breakdown", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedAllocationForExplanation?.let { alloc ->
        val invoice = invoices.firstOrNull { it.id == alloc.invoiceId }
        CostCalculationDialog(
            allocation = alloc,
            totalBudget = invoice?.totalAmount ?: 1500.0,
            totalSessions = invoice?.totalApprovedSessions ?: 30,
            onDismiss = { selectedAllocationForExplanation = null }
        )
    }

    selectedAllocationForDispute?.let { alloc ->
        val invoice = invoices.firstOrNull { it.id == alloc.invoiceId }
        RaiseDisputeDialog(
            initialCategory = "INVOICE_OVERCHARGE",
            initialReferenceType = "INVOICE_ALLOCATION",
            initialReferenceId = alloc.id,
            initialTitle = "Dispute on Invoice #${invoice?.invoiceNumber ?: "INV-${alloc.invoiceId}"}",
            initialRequestedAmount = alloc.allocatedAmount,
            contextSummaryText = "Invoice: ${invoice?.title ?: "Budget"} ($${String.format(Locale.US, "%.2f", alloc.allocatedAmount)} for ${alloc.approvedSessionsCount} sessions)",
            onDismiss = { selectedAllocationForDispute = null },
            onSubmit = { category, refType, refId, title, desc, reqAmt ->
                viewModel.raiseDispute(
                    category = category,
                    referenceType = refType,
                    referenceId = refId,
                    title = title,
                    description = desc,
                    requestedAdjustmentAmount = reqAmt
                )
                selectedAllocationForDispute = null
            }
        )
    }
}
