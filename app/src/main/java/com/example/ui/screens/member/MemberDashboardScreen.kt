package com.example.ui.screens.member

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.InvoiceAllocation
import com.example.data.entity.User
import com.example.ui.components.CostCalculationDialog
import com.example.ui.components.StatusChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.example.ui.components.ShareTeamJoinLinkDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    onNavigateToAttendance: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onOpenSubmitAttendance: () -> Unit,
    onOpenSubmitPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentBalance by viewModel.currentUserBalance.collectAsState()
    val attendances by viewModel.currentUserAttendances.collectAsState()
    val payments by viewModel.currentUserPayments.collectAsState()
    val allocations by viewModel.currentUserAllocations.collectAsState()
    val invoices by viewModel.allInvoices.collectAsState()
    val teams by viewModel.allTeams.collectAsState()

    var selectedAllocationForExplanation by remember { mutableStateOf<InvoiceAllocation?>(null) }
    var teamToShare by remember { mutableStateOf<com.example.data.entity.Team?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val approvedAttendanceCount = attendances.count { it.status == "APPROVED" }
    val pendingAttendanceCount = attendances.count { it.status == "PENDING" }
    val pendingPaymentsSum = payments.filter { it.status == "PENDING" }.sumOf { it.amount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Image Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_sports_club),
                    contentDescription = "Club Sports Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, SecondaryNavy.copy(alpha = 0.85f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Welcome, ${currentUser.name}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Member Contribution & Attendance Portal",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Running Balance Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        currentBalance > 0.01 -> Color(0xFF0F3E37)
                        currentBalance < -0.01 -> Color(0xFF3F1D1D)
                        else -> SecondaryNavy
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("member_balance_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MY CONTRIBUTION BALANCE",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = when {
                                currentBalance > 0.01 -> SuccessContainer
                                currentBalance < -0.01 -> ErrorContainer
                                else -> PrimaryContainer
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when {
                                    currentBalance > 0.01 -> "OVERPAID (CREDIT)"
                                    currentBalance < -0.01 -> "AMOUNT DUE"
                                    else -> "SETTLED"
                                },
                                color = when {
                                    currentBalance > 0.01 -> SuccessGreen
                                    currentBalance < -0.01 -> ErrorRed
                                    else -> OnPrimaryContainer
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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

                    Text(
                        text = when {
                            currentBalance > 0.01 -> "You have surplus credit which will automatically roll forward to next month's invoice."
                            currentBalance < -0.01 -> "Please submit your payment record to clear this outstanding allocation."
                            else -> "All allocated session charges are completely paid up."
                        },
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (pendingPaymentsSum > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarningContainer.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", pendingPaymentsSum)} payment pending admin verification",
                                color = WarningAmber,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons inside Balance Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenSubmitPayment,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentBalance < -0.01) GoldAccent else PrimaryGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("submit_payment_quick_btn")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Payment")
                        }

                        OutlinedButton(
                            onClick = onOpenSubmitAttendance,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("submit_attendance_quick_btn")
                        ) {
                            Icon(Icons.Default.Sports, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Attendance")
                        }
                    }
                }
            }
        }

        // Attendance & Participation Stats
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToAttendance() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SuccessContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Approved", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$approvedAttendanceCount Sessions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Counted toward costs", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToAttendance() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(WarningContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pending", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$pendingAttendanceCount Sessions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Awaiting verification", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        // Team Membership & Invite Link Strip
        val userTeam = attendances.firstOrNull()?.teamId?.let { tId -> teams.firstOrNull { it.id == tId } } ?: teams.firstOrNull()
        if (userTeam != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(userTeam.colorHex).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color(userTeam.colorHex), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = userTeam.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${userTeam.sportType} • Code: ${userTeam.inviteCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { teamToShare = userTeam },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = InfoContainer,
                                contentColor = InfoBlue
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("member_share_team_link_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invite Link")
                        }
                    }
                }
            }
        }

        // "How My Cost Was Calculated" Explanation Banner
        if (allocations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            selectedAllocationForExplanation = allocations.firstOrNull()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "How My Cost Was Calculated",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnPrimaryContainer
                            )
                            val latestAlloc = allocations.first()
                            Text(
                                text = "Latest: ${latestAlloc.approvedSessionsCount} sessions (${latestAlloc.percentage}%) = $${String.format(Locale.US, "%.2f", latestAlloc.allocatedAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryDark
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryGreen)
                    }
                }
            }
        }

        // Active Invoices & Allocations
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Invoices & Allocations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToInvoices) {
                    Text("View All", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (allocations.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "No invoices generated yet for this period.",
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(allocations.take(2)) { alloc ->
                val invoice = invoices.firstOrNull { it.id == alloc.invoiceId }
                val team = teams.firstOrNull { it.id == alloc.teamId }
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${team?.name ?: "Team"} • ${sdf.format(Date(alloc.calculatedAt))}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", alloc.allocatedAmount)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${alloc.approvedSessionsCount} approved sessions (${alloc.percentage}% of team)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(
                                onClick = { selectedAllocationForExplanation = alloc },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Explain Math", style = MaterialTheme.typography.labelLarge, color = PrimaryGreen)
                            }
                        }
                    }
                }
            }
        }

        // Recent Payments
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Payments",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToPayments) {
                    Text("Payment History", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (payments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "No payment records submitted yet.",
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(payments.take(3)) { payment ->
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (payment.status == "APPROVED") SuccessContainer else WarningContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (payment.status == "APPROVED") Icons.Default.Check else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (payment.status == "APPROVED") SuccessGreen else WarningAmber
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", payment.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
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
                }
            }
        }
    }

    // Cost Explanation Modal
    selectedAllocationForExplanation?.let { alloc ->
        val invoice = invoices.firstOrNull { it.id == alloc.invoiceId }
        CostCalculationDialog(
            allocation = alloc,
            totalBudget = invoice?.totalAmount ?: 1500.0,
            totalSessions = invoice?.totalApprovedSessions ?: 30,
            onDismiss = { selectedAllocationForExplanation = null }
        )
    }

    // Share Team Join Link Modal
    teamToShare?.let { team ->
        ShareTeamJoinLinkDialog(
            team = team,
            onDismiss = { teamToShare = null }
        )
    }
}
