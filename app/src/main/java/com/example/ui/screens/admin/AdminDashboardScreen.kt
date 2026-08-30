package com.example.ui.screens.admin

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
import com.example.data.entity.User
import com.example.ui.components.IssuePaymentRequestDialog
import com.example.ui.components.RecordBatchAttendanceDialog
import com.example.ui.components.StatKpiCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    onNavigateToApprovals: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memberSummaries by viewModel.memberSummaries.collectAsState()
    val pendingAttendances by viewModel.pendingAttendances.collectAsState()
    val pendingPayments by viewModel.pendingPayments.collectAsState()
    val openDisputes by viewModel.openDisputes.collectAsState()
    val teamStats by viewModel.teamDashboardStats.collectAsState()
    val allTeams by viewModel.allTeams.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var showBatchAttendanceDialog by remember { mutableStateOf(false) }
    var showPaymentRequestDialog by remember { mutableStateOf(false) }

    val totalOutstandingOwing = memberSummaries.filter { it.balance < -0.01 }.sumOf { -it.balance }
    val totalCollected = memberSummaries.sumOf { it.totalApprovedPaid }
    val totalPendingApprovals = pendingAttendances.size + pendingPayments.size + openDisputes.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Club Treasury & Admin Hub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Multi-team cost allocations, ledger audits, and member balances.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Pending approvals alert banner if any
        if (totalPendingApprovals > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToApprovals() }
                        .testTag("pending_approvals_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(WarningAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PendingActions, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$totalPendingApprovals Submissions & Disputes Awaiting Review",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                                Text(
                                    text = "${pendingAttendances.size} attendances • ${pendingPayments.size} payments • ${openDisputes.size} disputes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnGoldContainer
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WarningAmber)
                    }
                }
            }
        }

        // KPI Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatKpiCard(
                    title = "Outstanding Due",
                    value = "$${String.format(Locale.US, "%.2f", totalOutstandingOwing)}",
                    subtitle = "${memberSummaries.count { it.balance < -0.01 }} members owe fees",
                    icon = Icons.Default.MoneyOff,
                    containerColor = if (totalOutstandingOwing > 0) ErrorContainer.copy(alpha = 0.6f) else SurfaceCard,
                    iconColor = ErrorRed,
                    modifier = Modifier.weight(1f)
                )

                StatKpiCard(
                    title = "Collected Fees",
                    value = "$${String.format(Locale.US, "%.2f", totalCollected)}",
                    subtitle = "Approved & deposited",
                    icon = Icons.Default.Savings,
                    containerColor = SuccessContainer.copy(alpha = 0.6f),
                    iconColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Team Owner Quick Tools
        item {
            Text(
                text = "⚡ Team Owner Fast Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SecondaryNavy
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessContainer.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showBatchAttendanceDialog = true }
                        .testTag("dashboard_mark_attendance_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Attendance", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Check off roster for practice/match in 1 tap", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningContainer.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showPaymentRequestDialog = true }
                        .testTag("dashboard_payment_request_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RequestQuote, contentDescription = null, tint = WarningAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Issue Dues Request", color = WarningAmber, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Request uniforms, entry fees & copy text", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Quick Navigation Tiles
        item {
            Text(
                text = "Management Workflows",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToBudgets() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Budgets & Invoicing", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Allocate costs", color = PrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToApprovals() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Approvals Queue", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${pendingAttendances.size + pendingPayments.size} pending", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToReports() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Audit & Export", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Export CSVs", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Teams Budget Overview
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Teams & Monthly Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToBudgets) {
                    Text("Manage Budgets", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(teamStats) { stat ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                text = stat.team.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stat.team.sportType} • ${stat.totalApprovedSessionsThisMonth} approved sessions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            color = Color(stat.team.colorHex).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Goal: $${String.format(Locale.US, "%.0f", stat.totalBudgetValue)}",
                                color = Color(stat.team.colorHex),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val progress = if (stat.totalBudgetValue > 0) (stat.totalCollected / stat.totalBudgetValue).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PrimaryGreen,
                        trackColor = SurfaceLight
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Collected: $${String.format(Locale.US, "%.2f", stat.totalCollected)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = SuccessGreen
                        )
                        Text(
                            text = "Outstanding: $${String.format(Locale.US, "%.2f", stat.totalOutstanding)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (stat.totalOutstanding > 0) ErrorRed else TextSecondary
                        )
                    }
                }
            }
        }

        // Member Balances Snapshot
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Member Balances Snapshot",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToMembers) {
                    Text("All Members", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(memberSummaries) { member ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(user = member.user, size = 36)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = member.user.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${member.totalApprovedSessions} sessions • Paid: $${String.format(Locale.US, "%.2f", member.totalApprovedPaid)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = when {
                                member.balance > 0.01 -> "+$${String.format(Locale.US, "%.2f", member.balance)}"
                                member.balance < -0.01 -> "$${String.format(Locale.US, "%.2f", -member.balance)}"
                                else -> "$0.00"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                member.balance > 0.01 -> SuccessGreen
                                member.balance < -0.01 -> ErrorRed
                                else -> TextSecondary
                            }
                        )
                        Text(
                            text = when {
                                member.balance > 0.01 -> "Credit"
                                member.balance < -0.01 -> "Owes"
                                else -> "Settled"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showBatchAttendanceDialog) {
        RecordBatchAttendanceDialog(
            teams = allTeams,
            members = allUsers,
            onDismiss = { showBatchAttendanceDialog = false },
            onSubmit = { teamId, sessionDate, sessionType, notes, presentUserIds ->
                viewModel.recordBatchOwnerAttendance(
                    teamId = teamId,
                    sessionDate = sessionDate,
                    sessionType = sessionType,
                    notes = notes,
                    presentUserIds = presentUserIds
                )
            }
        )
    }

    if (showPaymentRequestDialog) {
        IssuePaymentRequestDialog(
            members = allUsers,
            preSelectedUser = null,
            onDismiss = { showPaymentRequestDialog = false },
            onSubmit = { userIds, title, amount, memo ->
                viewModel.issuePaymentRequest(
                    userIds = userIds,
                    title = title,
                    amount = amount,
                    memo = memo
                )
            }
        )
    }
}
