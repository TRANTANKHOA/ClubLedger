package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Attendance
import com.example.data.entity.BalanceLedger
import com.example.data.entity.Payment
import com.example.data.entity.Team
import com.example.data.entity.User
import com.example.ui.components.IssuePaymentRequestDialog
import com.example.ui.components.JoinTeamWithLinkDialog
import com.example.ui.components.ShareTeamJoinLinkDialog
import com.example.ui.components.StatusChip
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import com.example.ui.viewmodel.MemberSummary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMembersScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    val memberSummaries by viewModel.memberSummaries.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val teams by viewModel.allTeams.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()
    val allLedgerEntries by viewModel.allLedgerEntries.collectAsState()
    val allAttendances by viewModel.allAttendances.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMemberForAdjustment by remember { mutableStateOf<MemberSummary?>(null) }
    var selectedMemberForPaymentRequest by remember { mutableStateOf<User?>(null) }
    var selectedMemberForHistory by remember { mutableStateOf<MemberSummary?>(null) }
    var showPaymentRequestDialog by remember { mutableStateOf(false) }
    var showRegisterMemberDialog by remember { mutableStateOf(false) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var teamToShareJoinLink by remember { mutableStateOf<Team?>(null) }
    var showJoinTeamDialog by remember { mutableStateOf(false) }

    val filteredMembers = memberSummaries.filter {
        it.user.name.contains(searchQuery, ignoreCase = true) ||
                it.user.email.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showCreateTeamDialog = true },
                    containerColor = SecondaryNavy,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "New Team")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SmallFloatingActionButton(
                    onClick = {
                        selectedMemberForPaymentRequest = null
                        showPaymentRequestDialog = true
                    },
                    containerColor = WarningAmber,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.RequestQuote, contentDescription = "Issue Payment Request")
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExtendedFloatingActionButton(
                    onClick = { showRegisterMemberDialog = true },
                    containerColor = PrimaryGreen,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Register Member") },
                    modifier = Modifier.testTag("register_member_fab")
                )
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Members & Team Roster",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage member profiles, view ledger balances, and perform manual adjustments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search member by name or email...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_search_input")
                )
            }

            // Teams list strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Club Teams & Join Links (${teams.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { showCreateTeamDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Team")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    teams.forEach { team ->
                        Surface(
                            color = Color(team.colorHex).copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(team.colorHex).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(team.colorHex).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(team.colorHex), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(team.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(
                                            text = "${team.sportType} • Code: ${team.inviteCode.ifBlank { "TEAM-${team.id}" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { teamToShareJoinLink = team },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(team.colorHex).copy(alpha = 0.15f),
                                        contentColor = Color(team.colorHex)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("share_team_link_btn_${team.id}")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Join Link", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Members Roster (${filteredMembers.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(filteredMembers) { member ->
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
                                UserAvatar(user = member.user, size = 42)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = member.user.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = member.user.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    if (member.user.phone.isNotBlank()) {
                                        Text(
                                            text = member.user.phone,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = when {
                                        member.balance > 0.01 -> "+$${String.format(Locale.US, "%.2f", member.balance)}"
                                        member.balance < -0.01 -> "-$${String.format(Locale.US, "%.2f", -member.balance)}"
                                        else -> "$0.00"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        member.balance > 0.01 -> SuccessGreen
                                        member.balance < -0.01 -> ErrorRed
                                        else -> TextSecondary
                                    }
                                )
                                Text(
                                    text = member.statusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = SurfaceLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sessions: ${member.totalApprovedSessions} approved", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Paid: $${String.format(Locale.US, "%.2f", member.totalApprovedPaid)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = SuccessGreen)
                                Text("Invoiced: $${String.format(Locale.US, "%.2f", member.totalAllocatedCost)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { selectedMemberForHistory = member },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("History & Ledger", style = MaterialTheme.typography.labelMedium)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            FilledTonalButton(
                                onClick = {
                                    selectedMemberForPaymentRequest = member.user
                                    showPaymentRequestDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = WarningContainer.copy(alpha = 0.7f),
                                    contentColor = WarningAmber
                                )
                            ) {
                                Icon(Icons.Default.RequestQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Request Dues", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedButton(
                                onClick = { selectedMemberForAdjustment = member },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adjust", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Member History & Statement Dialog
    selectedMemberForHistory?.let { member ->
        val memberPayments = remember(allPayments, member.user.id) {
            allPayments.filter { it.userId == member.user.id }
        }
        val memberLedger = remember(allLedgerEntries, member.user.id) {
            allLedgerEntries.filter { it.userId == member.user.id }
        }
        val memberAttendances = remember(allAttendances, member.user.id) {
            allAttendances.filter { it.userId == member.user.id }
        }
        val memberTeam = teams.firstOrNull()

        MemberHistoryDialog(
            member = member,
            team = memberTeam,
            payments = memberPayments,
            ledger = memberLedger,
            attendances = memberAttendances,
            onDismiss = { selectedMemberForHistory = null }
        )
    }

    // Manual Balance Adjustment Dialog
    selectedMemberForAdjustment?.let { member ->
        ManualAdjustmentDialog(
            member = member,
            onDismiss = { selectedMemberForAdjustment = null },
            onSubmit = { adjType, amount, reason ->
                viewModel.applyManualAdjustment(member.user.id, adjType, amount, reason)
                selectedMemberForAdjustment = null
            }
        )
    }

    // Register Member Dialog
    if (showRegisterMemberDialog) {
        RegisterMemberDialog(
            teams = teams,
            onDismiss = { showRegisterMemberDialog = false },
            onSubmit = { name, email, phone, teamId ->
                viewModel.registerNewMember(name, email, phone, teamId)
                showRegisterMemberDialog = false
            }
        )
    }

    // Create Team Dialog
    if (showCreateTeamDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateTeamDialog = false },
            onSubmit = { name, sport, desc, budget, inviteCode ->
                viewModel.createNewTeam(name, sport, desc, budget, inviteCode)
                showCreateTeamDialog = false
            }
        )
    }

    // Share Team Join Link Dialog
    teamToShareJoinLink?.let { team ->
        ShareTeamJoinLinkDialog(
            team = team,
            onDismiss = { teamToShareJoinLink = null }
        )
    }

    // Join Team With Link Dialog (for testing join link submission)
    if (showJoinTeamDialog) {
        JoinTeamWithLinkDialog(
            viewModel = viewModel,
            currentUser = currentUser,
            onDismiss = { showJoinTeamDialog = false }
        )
    }

    // Issue Payment Request Dialog
    if (showPaymentRequestDialog) {
        IssuePaymentRequestDialog(
            members = allUsers,
            preSelectedUser = selectedMemberForPaymentRequest,
            onDismiss = {
                showPaymentRequestDialog = false
                selectedMemberForPaymentRequest = null
            },
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

@Composable
fun ManualAdjustmentDialog(
    member: MemberSummary,
    onDismiss: () -> Unit,
    onSubmit: (adjustmentType: String, amount: Double, reason: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("CREDIT") } // "CREDIT", "PENALTY", "REFUND", "CORRECTION"
    var amountText by remember { mutableStateOf("25.00") }
    var reason by remember { mutableStateOf("") }

    val types = listOf(
        "CREDIT" to "Credit (+ Balance)",
        "PENALTY" to "Penalty / Late Fee (- Balance)",
        "REFUND" to "Refund (+ Balance)",
        "CORRECTION" to "Audit Correction"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manual Balance Adjustment", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Adjusting balance for: ${member.user.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Current Balance: $${String.format(Locale.US, "%.2f", member.balance)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                Spacer(modifier = Modifier.height(4.dp))
                Text("Adjustment Type:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Column {
                    types.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedType == key) PrimaryContainer else Color.Transparent)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            RadioButton(selected = selectedType == key, onClick = { selectedType = key })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Adjustment Amount ($)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Audit Note") },
                    placeholder = { Text("e.g. Referee discount or equipment credit") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && reason.isNotBlank()) {
                        onSubmit(selectedType, amount, reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Apply & Log in Ledger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RegisterMemberDialog(
    teams: List<Team>,
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, phone: String, teamId: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register Club Member", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_member_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_member_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Assign to Team:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Column {
                    teams.forEach { team ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTeamId == team.id) PrimaryContainer else Color.Transparent)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            RadioButton(selected = selectedTeamId == team.id, onClick = { selectedTeamId = team.id })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(team.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                val context = LocalContext.current
                val clipboardManager = LocalClipboardManager.current
                val selectedTeamName = teams.firstOrNull { it.id == selectedTeamId }?.name ?: "our club"

                Surface(
                    color = InfoContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Share Quick Invite Message", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = InfoBlue)
                            Text("Copy WhatsApp / SMS invite template for new member.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        IconButton(
                            onClick = {
                                val inviteMsg = "🏆 Hi ${name.ifBlank { "there" }}! You've been invited to join $selectedTeamName on ClubLedger. Track your team attendance, session fees, and dues transparently at: https://clubledger.app/join"
                                clipboardManager.setText(AnnotatedString(inviteMsg))
                                Toast.makeText(context, "Invite message copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Invite", tint = InfoBlue)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onSubmit(name, email, phone, selectedTeamId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("submit_register_member_btn")
            ) {
                Text("Register Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, sport: String, desc: String, budget: Double, inviteCode: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("Soccer") }
    var desc by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("1200.00") }
    var customInviteCode by remember { mutableStateOf("") }

    val generatedCodePreview = remember(name, customInviteCode) {
        if (customInviteCode.isNotBlank()) customInviteCode.trim().uppercase()
        else {
            val prefix = name.uppercase().replace(Regex("[^A-Z0-9]"), "").take(6).ifBlank { "TEAM" }
            "$prefix-2026"
        }
    }

    val sports = listOf("Soccer", "Basketball", "Volleyball", "Running", "Tennis", "Badminton")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Team", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Team Name *") },
                    placeholder = { Text("e.g. Riverside Masters") },
                    modifier = Modifier.fillMaxWidth().testTag("new_team_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Sport Type:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sports.take(3).forEach { s ->
                        FilterChip(
                            selected = sport == s,
                            onClick = { sport = s },
                            label = { Text(s) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customInviteCode,
                    onValueChange = { customInviteCode = it.uppercase() },
                    label = { Text("Team Invite Code (Optional)") },
                    placeholder = { Text("e.g. $generatedCodePreview") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = PrimaryGreen) },
                    modifier = Modifier.fillMaxWidth().testTag("new_team_invite_code_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Surface(
                    color = InfoContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Join link: https://clubledger.app/join?team=$generatedCodePreview",
                            style = MaterialTheme.typography.labelSmall,
                            color = InfoBlue,
                            maxLines = 1
                        )
                    }
                }

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Monthly Budget Goal ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val budget = budgetText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onSubmit(name, sport, desc, budget, customInviteCode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("submit_create_team_btn")
            ) {
                Text("Create Team & Generate Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MemberHistoryDialog(
    member: MemberSummary,
    team: Team?,
    payments: List<Payment>,
    ledger: List<BalanceLedger>,
    attendances: List<Attendance>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)
    val dateOnlySdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    var selectedTab by remember { mutableStateOf(0) } // 0 = Payments, 1 = Ledger, 2 = Attendance

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCard,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(user = member.user, size = 48)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = member.user.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${team?.name ?: "Unassigned"} • ${member.user.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // KPI Overview Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Balance", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = when {
                                    member.balance > 0.01 -> "+$${String.format(Locale.US, "%.2f", member.balance)}"
                                    member.balance < -0.01 -> "-$${String.format(Locale.US, "%.2f", -member.balance)}"
                                    else -> "$0.00"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    member.balance > 0.01 -> SuccessGreen
                                    member.balance < -0.01 -> ErrorRed
                                    else -> TextSecondary
                                }
                            )
                        }

                        Divider(modifier = Modifier.height(36.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Paid", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", member.totalApprovedPaid)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Divider(modifier = Modifier.height(36.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Invoiced", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", member.totalAllocatedCost)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Payments (${payments.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Ledger (${ledger.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Attendance (${attendances.size})", fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> {
                            if (payments.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No payments submitted by this member yet.", color = TextSecondary)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(payments) { p ->
                                        Surface(
                                            color = SurfaceLight,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "$${String.format(Locale.US, "%.2f", p.amount)}",
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = SuccessGreen
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "via ${p.paymentMethod.replace("_", " ")}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                    if (p.referenceNote.isNotBlank()) {
                                                        Text(
                                                            text = "Ref: ${p.referenceNote}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                    Text(
                                                        text = sdf.format(Date(p.paymentDate)),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextMuted
                                                    )
                                                }
                                                StatusChip(status = p.status)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (ledger.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No ledger transactions recorded.", color = TextSecondary)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(ledger) { entry ->
                                        Surface(
                                            color = SurfaceLight,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = entry.description,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "${entry.type.replace("_", " ")} • ${sdf.format(Date(entry.createdAt))}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextMuted
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = if (entry.amount >= 0) "+$${String.format(Locale.US, "%.2f", entry.amount)}" else "-$${String.format(Locale.US, "%.2f", -entry.amount)}",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = if (entry.amount >= 0) SuccessGreen else ErrorRed
                                                    )
                                                    Text(
                                                        text = "Bal: $${String.format(Locale.US, "%.2f", entry.runningBalanceAfter)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (attendances.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No attendance logs found.", color = TextSecondary)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(attendances) { a ->
                                        Surface(
                                            color = SurfaceLight,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${a.sessionType} ${if (a.notes.isNotBlank()) "• ${a.notes}" else ""}",
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = dateOnlySdf.format(Date(a.sessionDate)),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                                StatusChip(status = a.status)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val statement = buildString {
                                appendLine("📋 ACCOUNT STATEMENT - ${member.user.name.uppercase()}")
                                appendLine("Team: ${team?.name ?: "General"}")
                                appendLine("Email: ${member.user.email}")
                                appendLine("----------------------------------------")
                                appendLine("Total Sessions Attended: ${member.totalApprovedSessions}")
                                appendLine("Total Invoiced: $${String.format(Locale.US, "%.2f", member.totalAllocatedCost)}")
                                appendLine("Total Approved Paid: $${String.format(Locale.US, "%.2f", member.totalApprovedPaid)}")
                                appendLine("Current Running Balance: $${String.format(Locale.US, "%.2f", member.balance)}")
                                appendLine("Status: ${member.statusText}")
                                appendLine("----------------------------------------")
                                appendLine("Generated via ClubLedger")
                            }
                            clipboardManager.setText(AnnotatedString(statement))
                            Toast.makeText(context, "Statement copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Statement")
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
