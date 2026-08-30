package com.example.ui.screens.member

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
import com.example.data.entity.Team
import com.example.data.entity.User
import com.example.ui.components.RaiseDisputeDialog
import com.example.ui.components.StatusChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberAttendanceScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    val attendances by viewModel.currentUserAttendances.collectAsState()
    val teams by viewModel.allTeams.collectAsState()
    val userDisputes by viewModel.currentUserDisputes.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var selectedAttendanceForDispute by remember { mutableStateOf<Attendance?>(null) }

    val filteredAttendances = when (selectedFilter) {
        "APPROVED" -> attendances.filter { it.status == "APPROVED" }
        "PENDING" -> attendances.filter { it.status == "PENDING" }
        "REJECTED" -> attendances.filter { it.status == "REJECTED" }
        else -> attendances
    }

    val approvedCount = attendances.count { it.status == "APPROVED" }
    val pendingCount = attendances.count { it.status == "PENDING" }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSubmitDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log Attendance") },
                modifier = Modifier.testTag("log_attendance_fab")
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
                    text = "My Attendance Records",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Only approved attendance counts towards monthly cost allocation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Overview stats banner
            item {
                Surface(
                    color = PrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Approved Sessions", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = "$approvedCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text("Active for cost sharing", style = MaterialTheme.typography.bodyMedium, color = PrimaryDark)
                        }
                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = PrimaryLight.copy(alpha = 0.3f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Pending Verification", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = "$pendingCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                            Text("Awaiting admin review", style = MaterialTheme.typography.bodyMedium, color = PrimaryDark)
                        }
                    }
                }
            }

            // Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${attendances.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "APPROVED",
                        onClick = { selectedFilter = "APPROVED" },
                        label = { Text("Approved ($approvedCount)") }
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Pending ($pendingCount)") }
                    )
                    val rejectedCount = attendances.count { it.status == "REJECTED" }
                    if (rejectedCount > 0) {
                        FilterChip(
                            selected = selectedFilter == "REJECTED",
                            onClick = { selectedFilter = "REJECTED" },
                            label = { Text("Rejected ($rejectedCount)") }
                        )
                    }
                }
            }

            if (filteredAttendances.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No attendance entries found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tap 'Log Attendance' below to submit a session record.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredAttendances) { attendance ->
                    val existingDispute = userDisputes.firstOrNull { it.referenceType == "ATTENDANCE" && it.referenceId == attendance.id }
                    AttendanceItemCard(
                        attendance = attendance,
                        teams = teams,
                        existingDispute = existingDispute,
                        onDisputeClick = { selectedAttendanceForDispute = attendance }
                    )
                }
            }
        }
    }

    if (showSubmitDialog) {
        SubmitAttendanceDialog(
            teams = teams,
            onDismiss = { showSubmitDialog = false },
            onSubmit = { teamId, dateMillis, sessionType, notes ->
                viewModel.submitAttendance(teamId, dateMillis, sessionType, notes)
                showSubmitDialog = false
            }
        )
    }

    selectedAttendanceForDispute?.let { attendance ->
        val team = teams.firstOrNull { it.id == attendance.teamId }
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        RaiseDisputeDialog(
            initialCategory = "ATTENDANCE_DISCREPANCY",
            initialReferenceType = "ATTENDANCE",
            initialReferenceId = attendance.id,
            initialTitle = "Dispute on ${attendance.sessionType} attendance on ${sdf.format(Date(attendance.sessionDate))}",
            initialRequestedAmount = 0.0,
            contextSummaryText = "${attendance.sessionType} on ${sdf.format(Date(attendance.sessionDate))} (${team?.name ?: "Team"}) - Status: ${attendance.status}",
            onDismiss = { selectedAttendanceForDispute = null },
            onSubmit = { category, refType, refId, title, desc, reqAmt ->
                viewModel.raiseDispute(
                    category = category,
                    referenceType = refType,
                    referenceId = refId,
                    title = title,
                    description = desc,
                    requestedAdjustmentAmount = reqAmt
                )
                selectedAttendanceForDispute = null
            }
        )
    }
}

@Composable
fun AttendanceItemCard(
    attendance: Attendance,
    teams: List<Team>,
    existingDispute: Dispute? = null,
    onDisputeClick: () -> Unit = {}
) {
    val team = teams.firstOrNull { it.id == attendance.teamId }
    val sdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)

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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (attendance.sessionType) {
                                    "MATCH" -> GoldContainer
                                    "TOURNAMENT" -> InfoContainer
                                    else -> PrimaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (attendance.sessionType) {
                                "MATCH" -> Icons.Default.EmojiEvents
                                "TOURNAMENT" -> Icons.Default.MilitaryTech
                                else -> Icons.Default.FitnessCenter
                            },
                            contentDescription = null,
                            tint = when (attendance.sessionType) {
                                "MATCH" -> OnGoldContainer
                                "TOURNAMENT" -> InfoBlue
                                else -> PrimaryGreen
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = attendance.sessionType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = team?.name ?: "Sports Team",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                StatusChip(status = attendance.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = sdf.format(Date(attendance.sessionDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (attendance.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${attendance.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (!attendance.reviewNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (attendance.status == "APPROVED") SuccessContainer.copy(alpha = 0.5f) else ErrorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (attendance.status == "APPROVED") Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (attendance.status == "APPROVED") SuccessGreen else ErrorRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admin Review: ${attendance.reviewNotes}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (attendance.status == "APPROVED") SuccessGreen else ErrorRed
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
            } else if (attendance.status == "REJECTED" || attendance.status == "PENDING") {
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
                        Text("Raise Dispute", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitAttendanceDialog(
    teams: List<Team>,
    onDismiss: () -> Unit,
    onSubmit: (teamId: Long, dateMillis: Long, sessionType: String, notes: String) -> Unit
) {
    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id ?: 1L) }
    var selectedSessionType by remember { mutableStateOf("TRAINING") }
    var notes by remember { mutableStateOf("") }
    var dayOffset by remember { mutableStateOf(0) } // 0 = today, -1 = yesterday, etc.

    val sessionTypes = listOf("TRAINING", "MATCH", "TOURNAMENT", "FRIENDLY", "SOCIAL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsScore, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Attendance Record", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Team:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Column {
                    teams.forEach { team ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTeamId == team.id) PrimaryContainer else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedTeamId == team.id,
                                onClick = { selectedTeamId = team.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(team.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Text(
                    text = "Session Date:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = dayOffset == 0,
                        onClick = { dayOffset = 0 },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = dayOffset == -1,
                        onClick = { dayOffset = -1 },
                        label = { Text("Yesterday") }
                    )
                    FilterChip(
                        selected = dayOffset == -2,
                        onClick = { dayOffset = -2 },
                        label = { Text("2 days ago") }
                    )
                }

                Text(
                    text = "Session Type:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sessionTypes.take(3).forEach { type ->
                        FilterChip(
                            selected = selectedSessionType == type,
                            onClick = { selectedSessionType = type },
                            label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sessionTypes.drop(3).forEach { type ->
                        FilterChip(
                            selected = selectedSessionType == type,
                            onClick = { selectedSessionType = type },
                            label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Drills (optional)") },
                    placeholder = { Text("e.g. Attended regular tactical drill") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, dayOffset)
                    onSubmit(selectedTeamId, cal.timeInMillis, selectedSessionType, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("submit_attendance_confirm_btn")
            ) {
                Text("Submit for Verification")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
