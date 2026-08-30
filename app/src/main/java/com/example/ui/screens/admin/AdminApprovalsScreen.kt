package com.example.ui.screens.admin

import androidx.compose.foundation.background
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
import com.example.data.entity.Attendance
import com.example.data.entity.Dispute
import com.example.data.entity.Payment
import com.example.data.entity.Team
import com.example.data.entity.User
import com.example.ui.components.RecordBatchAttendanceDialog
import com.example.ui.components.ResolveDisputeDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalsScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    val pendingAttendances by viewModel.pendingAttendances.collectAsState()
    val pendingPayments by viewModel.pendingPayments.collectAsState()
    val pendingJoinRequests by viewModel.pendingJoinRequests.collectAsState()
    val openDisputes by viewModel.openDisputes.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val teams by viewModel.allTeams.collectAsState()

    val usersMap = remember(users) { users.associateBy { it.id } }
    val teamsMap = remember(teams) { teams.associateBy { it.id } }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Attendance, 1 = Payments, 2 = Join Requests, 3 = Disputes
    var rejectingAttendanceId by remember { mutableStateOf<Long?>(null) }
    var rejectingPaymentId by remember { mutableStateOf<Long?>(null) }
    var rejectingJoinRequestId by remember { mutableStateOf<Long?>(null) }
    var resolvingDispute by remember { mutableStateOf<Dispute?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var showBatchAttendanceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Verification & Approvals Queue",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Verify submissions. Only approved records affect cost allocation, rosters, and balances.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = PrimaryGreen,
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Attendance (${pendingAttendances.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == 0) PrimaryGreen else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Payments (${pendingPayments.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == 1) PrimaryGreen else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Join Requests (${pendingJoinRequests.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == 2) PrimaryGreen else if (pendingJoinRequests.isNotEmpty()) InfoBlue else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = "Disputes (${openDisputes.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == 3) WarningAmber else TextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Owner Action Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsHandball, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Team Owner Session Tool",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Button(
                        onClick = { showBatchAttendanceDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryNavy),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("mark_session_attendance_btn")
                    ) {
                        Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Attendance", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // Pending Attendance List
            if (pendingAttendances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("All caught up!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("No pending member attendance submissions to review.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showBatchAttendanceDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Roster Attendance as Owner")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${pendingAttendances.size} Pending Submissions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { viewModel.bulkApproveAttendances() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("bulk_approve_attendances_btn")
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bulk Approve All")
                            }
                        }
                    }

                    items(pendingAttendances) { att ->
                        val member = usersMap[att.userId]
                        val team = teamsMap[att.teamId]
                        val sdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)

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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (member != null) {
                                            UserAvatar(user = member, size = 36)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = member?.name ?: "Unknown Member",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${team?.name ?: "Team"} • ${att.sessionType}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Surface(color = WarningContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "PENDING",
                                            color = WarningAmber,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Session Date: ${sdf.format(Date(att.sessionDate))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (att.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Notes: ${att.notes}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            rejectingAttendanceId = att.id
                                            rejectionReason = ""
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reject")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { viewModel.approveAttendance(att.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            // Pending Payments List
            if (pendingPayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("All payments verified!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("No pending payment submissions awaiting verification.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${pendingPayments.size} Payments Requiring Deposit Confirmation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(pendingPayments) { payment ->
                        val member = usersMap[payment.userId]
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (member != null) {
                                            UserAvatar(user = member, size = 38)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = member?.name ?: "Unknown Member",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${payment.paymentMethod.replace("_", " ")} • ${sdf.format(Date(payment.paymentDate))}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", payment.amount)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }

                                if (payment.referenceNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Ref / Memo: ${payment.referenceNote}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }

                                if (!payment.receiptNote.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Receipt: ${payment.receiptNote}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            rejectingPaymentId = payment.id
                                            rejectionReason = ""
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reject")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { viewModel.approvePayment(payment.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve & Credit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 2) {
            // Pending Join Requests List
            if (pendingJoinRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Pending Join Requests", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("When players use your team join links or codes, their applications will appear here for your approval.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            color = InfoContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Single link multi-join: Approving a request will immediately add the applicant to the team roster and register their member account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    items(pendingJoinRequests) { req ->
                        val targetTeam = teamsMap[req.teamId]
                        val sdf = SimpleDateFormat("EEE, MMM dd, yyyy • HH:mm", Locale.US)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_request_card_${req.id}")
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
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryGreen.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = req.applicantName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = req.applicantEmail + (if (req.applicantPhone.isNotBlank()) " • ${req.applicantPhone}" else ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Surface(
                                        color = InfoContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = targetTeam?.name ?: "Team #${req.teamId}",
                                            color = InfoBlue,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (req.message.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = SurfaceLight,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Applicant Note:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                                            Text(
                                                text = req.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Applied via Join Link • ${sdf.format(Date(req.createdAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            rejectingJoinRequestId = req.id
                                            rejectionReason = ""
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("decline_join_req_${req.id}")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Decline")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            viewModel.approveJoinRequest(req.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("approve_join_req_${req.id}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve & Add to Roster")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Open Disputes Review List
            if (openDisputes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Open Disputes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("All member disputes and fee inquiries have been resolved.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${openDisputes.size} Member Inquiries & Disputes Requiring Ruling",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(openDisputes) { dispute ->
                        val member = usersMap[dispute.userId]
                        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)

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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (member != null) {
                                            UserAvatar(user = member, size = 38)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = member?.name ?: "Member #${dispute.userId}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Category: ${dispute.category.replace("_", " ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Surface(color = WarningContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "OPEN",
                                            color = WarningAmber,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = dispute.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SecondaryNavy
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dispute.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )

                                if (dispute.requestedAdjustmentAmount > 0.0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = SuccessContainer.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Requested Balance Credit: $${String.format(Locale.US, "%.2f", dispute.requestedAdjustmentAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Submitted: ${sdf.format(Date(dispute.createdAt))} • Ref: ${dispute.referenceType} #${dispute.referenceId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { resolvingDispute = dispute },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryNavy),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("admin_resolve_dispute_${dispute.id}")
                                    ) {
                                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Review & Resolve Dispute")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rejection Dialog for Attendance
    rejectingAttendanceId?.let { attId ->
        AlertDialog(
            onDismissRequest = { rejectingAttendanceId = null },
            title = { Text("Reject Attendance Record") },
            text = {
                Column {
                    Text("Please specify why this attendance submission cannot be approved:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason") },
                        placeholder = { Text("e.g. Unregistered session or late absence") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectAttendance(attId, rejectionReason.ifBlank { "Rejected by admin" })
                        rejectingAttendanceId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingAttendanceId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rejection Dialog for Payment
    rejectingPaymentId?.let { payId ->
        AlertDialog(
            onDismissRequest = { rejectingPaymentId = null },
            title = { Text("Reject Payment Record") },
            text = {
                Column {
                    Text("Please specify why this payment record cannot be verified:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason") },
                        placeholder = { Text("e.g. Transaction ID not found on bank statement") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectPayment(payId, rejectionReason.ifBlank { "Transaction not verified in bank account" })
                        rejectingPaymentId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingPaymentId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rejection Dialog for Join Request
    rejectingJoinRequestId?.let { reqId ->
        AlertDialog(
            onDismissRequest = { rejectingJoinRequestId = null },
            title = { Text("Decline Join Request") },
            text = {
                Column {
                    Text("Optionally provide a reason for declining this team join application:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Reason (Optional)") },
                        placeholder = { Text("e.g. Roster full, or wrong age bracket") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectJoinRequest(reqId, rejectionReason.ifBlank { "Declined by team owner" })
                        rejectingJoinRequestId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Confirm Decline")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingJoinRequestId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    resolvingDispute?.let { dispute ->
        val member = usersMap[dispute.userId]
        ResolveDisputeDialog(
            dispute = dispute,
            member = member,
            onDismiss = { resolvingDispute = null },
            onResolve = { action, notes, creditAmount ->
                viewModel.resolveDispute(
                    disputeId = dispute.id,
                    resolutionAction = action,
                    resolutionNotes = notes,
                    creditAmount = creditAmount
                )
                resolvingDispute = null
            }
        )
    }

    if (showBatchAttendanceDialog) {
        RecordBatchAttendanceDialog(
            teams = teams,
            members = users,
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
}
