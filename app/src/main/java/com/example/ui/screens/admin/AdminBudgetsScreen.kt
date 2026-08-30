package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.entity.*
import com.example.ui.components.StatusChip
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBudgetsScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.allBudgets.collectAsState()
    val invoices by viewModel.allInvoices.collectAsState()
    val allocations by viewModel.allAllocations.collectAsState()
    val teams by viewModel.allTeams.collectAsState()
    val users by viewModel.allUsers.collectAsState()

    val teamsMap = remember(teams) { teams.associateBy { it.id } }
    val usersMap = remember(users) { users.associateBy { it.id } }

    var showCreateBudgetDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForDetails by remember { mutableStateOf<Invoice?>(null) }
    var recalculatingInvoiceId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateBudgetDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Team Budget") },
                modifier = Modifier.testTag("create_budget_fab")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Budgets & Cost Invoicing Engine",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Define monthly team expenses and automatically allocate them to members proportionally by verified attendance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Proportional Allocation Explanation Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoMode, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Automated Proportional Allocation Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnPrimaryContainer
                            )
                            Text(
                                text = "Formula: (Member Verified Sessions ÷ Total Team Sessions) × Total Budget. Cent-perfect rounding with automated ledger debits.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryDark
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Monthly Budgets & Invoices (${budgets.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (budgets.isEmpty()) {
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
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No team budgets created yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tap 'New Team Budget' to create your first monthly team budget.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(budgets) { budget ->
                    val team = teamsMap[budget.teamId]
                    val existingInvoice = invoices.firstOrNull { it.budgetId == budget.id }
                    val budgetAllocations = if (existingInvoice != null) allocations.filter { it.invoiceId == existingInvoice.id } else emptyList()

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
                                        text = budget.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${team?.name ?: "Team"} • Period: ${getMonthName(budget.periodMonth)} ${budget.periodYear}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                StatusChip(status = budget.status)
                            }

                            if (budget.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = budget.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
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
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Budget", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", budget.totalAmount)}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    if (existingInvoice != null) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Verified Sessions", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                            Text(
                                                text = "${existingInvoice.totalApprovedSessions}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGreen
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Rate / Session", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                            Text(
                                                text = "$${String.format(Locale.US, "%.2f", existingInvoice.costPerSession)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGreen
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action button logic
                            if (existingInvoice == null) {
                                Button(
                                    onClick = { viewModel.generateInvoiceAndAllocate(budget.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("generate_allocation_btn_${budget.id}")
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Invoice & Proportional Allocation")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedInvoiceForDetails = existingInvoice },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View Allocations (${budgetAllocations.size})")
                                    }

                                    Button(
                                        onClick = { recalculatingInvoiceId = existingInvoice.id },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryNavy),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Recalculate")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Team Budget Dialog
    if (showCreateBudgetDialog) {
        CreateBudgetDialog(
            teams = teams,
            onDismiss = { showCreateBudgetDialog = false },
            onSubmit = { teamId, title, desc, amount, month, year ->
                viewModel.createTeamBudget(teamId, title, desc, amount, month, year)
                showCreateBudgetDialog = false
            }
        )
    }

    // Recalculate confirmation dialog
    recalculatingInvoiceId?.let { invId ->
        AlertDialog(
            onDismissRequest = { recalculatingInvoiceId = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recalculate Cost Allocations?")
                }
            },
            text = {
                Text("This will recalculate all member shares based on the latest approved attendance records, adjust the balance ledger accordingly, and record an audit trail.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recalculateInvoice(invId)
                        recalculatingInvoiceId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Recalculate & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { recalculatingInvoiceId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Invoice Details & Allocation Breakdown Modal
    selectedInvoiceForDetails?.let { invoice ->
        val invoiceAllocations = allocations.filter { it.invoiceId == invoice.id }
        AlertDialog(
            onDismissRequest = { selectedInvoiceForDetails = null },
            title = {
                Column {
                    Text(invoice.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Invoice #${invoice.invoiceNumber}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(color = SecondaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Budget: $${String.format(Locale.US, "%.2f", invoice.totalAmount)}", fontWeight = FontWeight.Bold, color = SecondaryNavy)
                                Text("Sessions: ${invoice.totalApprovedSessions}", fontWeight = FontWeight.Bold, color = SecondaryNavy)
                            }
                        }
                    }

                    items(invoiceAllocations) { alloc ->
                        val member = usersMap[alloc.userId]
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (member != null) {
                                        UserAvatar(user = member, size = 30)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(member?.name ?: "Member #${alloc.userId}", fontWeight = FontWeight.SemiBold)
                                        Text("${alloc.approvedSessionsCount} sessions (${alloc.percentage}%)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", alloc.allocatedAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedInvoiceForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CreateBudgetDialog(
    teams: List<Team>,
    onDismiss: () -> Unit,
    onSubmit: (teamId: Long, title: String, desc: String, amount: Double, month: Int, year: Int) -> Unit
) {
    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id ?: 1L) }
    var title by remember { mutableStateOf("Monthly Pitch & Referee Expenses") }
    var description by remember { mutableStateOf("Facility rental, referees, and equipment") }
    var amountText by remember { mutableStateOf("1500.00") }

    val cal = Calendar.getInstance()
    var selectedMonth by remember { mutableStateOf(cal.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableStateOf(cal.get(Calendar.YEAR)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Monthly Team Budget", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Team:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Column {
                    teams.forEach { team ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTeamId == team.id) PrimaryContainer else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTeamId == team.id,
                                onClick = { selectedTeamId = team.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(team.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Budget Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total Budget Amount ($)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedMonth.toString(),
                        onValueChange = { selectedMonth = it.toIntOrNull() ?: 1 },
                        label = { Text("Month (1-12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = { selectedYear = it.toIntOrNull() ?: 2026 },
                        label = { Text("Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && title.isNotBlank()) {
                        onSubmit(selectedTeamId, title, description, amount, selectedMonth, selectedYear)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("submit_create_budget_btn")
            ) {
                Text("Create Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getMonthName(monthNumber: Int): String {
    return when (monthNumber) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month $monthNumber"
    }
}
