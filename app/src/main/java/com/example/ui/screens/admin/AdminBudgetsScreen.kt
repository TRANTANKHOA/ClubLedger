package com.example.ui.screens.admin

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
    val allPayments by viewModel.allPayments.collectAsState()

    val teamsMap = remember(teams) { teams.associateBy { it.id } }
    val usersMap = remember(users) { users.associateBy { it.id } }

    val totalIncurredCosts = remember(budgets) { budgets.sumOf { it.totalAmount } }
    val totalMemberCollections = remember(allPayments) {
        allPayments.filter { it.status == "APPROVED" }.sumOf { it.amount }
    }
    val netTreasuryBalance = totalMemberCollections - totalIncurredCosts

    var showCreateBudgetDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForDetails by remember { mutableStateOf<Invoice?>(null) }
    var selectedInvoiceForProof by remember { mutableStateOf<Invoice?>(null) }
    var selectedBudgetForProof by remember { mutableStateOf<TeamBudget?>(null) }
    var recalculatingInvoiceId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateBudgetDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Declare Cost Item") },
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
                    text = "Declare team expenses with attached invoice proofs & allocate costs fairly by verified attendance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Financial Balance Overview Card (Incurred Costs vs Member Collections)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TEAM TREASURY POSITION",
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (netTreasuryBalance >= 0) PrimaryGreen.copy(alpha = 0.25f) else WarningAmber.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (netTreasuryBalance >= 0) "SURPLUS 🟢" else "DEFICIT 🟠",
                                    color = if (netTreasuryBalance >= 0) Color(0xFF86EFAC) else WarningAmber,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Incurred Costs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", totalIncurredCosts)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Monthly Budget Sum",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    text = "Total Member Collections",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", totalMemberCollections)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC)
                                )
                                Text(
                                    text = "Approved Deposits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Pool Position:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "${if (netTreasuryBalance >= 0) "+" else ""}$${String.format(Locale.US, "%.2f", netTreasuryBalance)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netTreasuryBalance >= 0) Color(0xFF86EFAC) else WarningAmber
                            )
                        }
                    }
                }
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
                                text = "Formula: (Member Verified Sessions ÷ Total Team Sessions) × Incurred Cost. Zero attendance = $0.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryDark
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Declared Cost Items & Invoices (${budgets.size})",
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
                            Text("No cost items declared yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tap 'Declare Cost Item' to create your first monthly team expense and attach proof.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = when (budget.category) {
                                                "COURT_RENTAL" -> PrimaryContainer
                                                "TOURNAMENT" -> WarningContainer
                                                "EQUIPMENT" -> InfoContainer
                                                "COACHING_REFS" -> SecondaryContainer
                                                else -> Color(0xFFF1F5F9)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = when (budget.category) {
                                                    "COURT_RENTAL" -> "🏟️ COURT RENTAL"
                                                    "TOURNAMENT" -> "🏆 TOURNAMENT"
                                                    "EQUIPMENT" -> "⚽ EQUIPMENT"
                                                    "COACHING_REFS" -> "👨‍🏫 COACH/REF"
                                                    "TRANSPORT" -> "🚌 TRANSPORT"
                                                    else -> "📦 OPERATING"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = when (budget.category) {
                                                    "COURT_RENTAL" -> PrimaryDark
                                                    "TOURNAMENT" -> WarningAmber
                                                    "EQUIPMENT" -> InfoBlue
                                                    else -> TextPrimary
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${team?.name ?: "Team"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = budget.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Period: ${getMonthName(budget.periodMonth)} ${budget.periodYear}",
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
                                        Text("Total Incurred Cost", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
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

                            // Receipt Attachment Button
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBudgetForProof = budget
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = budget.attachmentName ?: "Invoice_Proof_Receipt.pdf",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryDark
                                        )
                                    }
                                    Text(
                                        text = "View Attached Proof >",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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
                                        Text("Allocations (${budgetAllocations.size})")
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

    // View Attached Invoice Proof Dialog
    selectedBudgetForProof?.let { budget ->
        val team = teamsMap[budget.teamId]
        val existingInvoice = invoices.firstOrNull { it.budgetId == budget.id }
        com.example.ui.components.ViewInvoiceAttachmentDialog(
            invoice = existingInvoice,
            budget = budget,
            team = team,
            onDismiss = { selectedBudgetForProof = null }
        )
    }

    // Create Team Budget & Declare Cost Item Dialog
    if (showCreateBudgetDialog) {
        CreateBudgetDialog(
            teams = teams,
            onDismiss = { showCreateBudgetDialog = false },
            onSubmit = { teamId, title, desc, amount, month, year, category, attachmentUrl, attachmentType, attachmentName ->
                viewModel.createTeamBudget(
                    teamId = teamId,
                    title = title,
                    description = desc,
                    amount = amount,
                    periodMonth = month,
                    periodYear = year,
                    category = category,
                    attachmentUrl = attachmentUrl,
                    attachmentType = attachmentType,
                    attachmentName = attachmentName
                )
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
    onSubmit: (
        teamId: Long,
        title: String,
        desc: String,
        amount: Double,
        month: Int,
        year: Int,
        category: String,
        attachmentUrl: String?,
        attachmentType: String?,
        attachmentName: String?
    ) -> Unit
) {
    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id ?: 1L) }
    var title by remember { mutableStateOf("Monthly Pitch Rental & Referees") }
    var description by remember { mutableStateOf("Facility pitch lease (4 weeks) + match referee fees") }
    var amountText by remember { mutableStateOf("1500.00") }
    var selectedCategory by remember { mutableStateOf("COURT_RENTAL") }

    val cal = Calendar.getInstance()
    var selectedMonth by remember { mutableStateOf(cal.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableStateOf(cal.get(Calendar.YEAR)) }

    // Preset attachment options
    var attachmentType by remember { mutableStateOf("RECEIPT_IMAGE") }
    var attachmentName by remember { mutableStateOf("Facility_Rental_Signed_Invoice.pdf") }
    var attachmentUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800&auto=format&fit=crop&q=60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Declare & Share Cost Item", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
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
                                .clickable { selectedTeamId = team.id }
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

                // Category Selection
                Text("Expense Category:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf(
                        "COURT_RENTAL" to "🏟️ Pitch/Court",
                        "TOURNAMENT" to "🏆 League",
                        "EQUIPMENT" to "⚽ Gear",
                        "COACHING_REFS" to "👨‍🏫 Referee"
                    )
                    categories.forEach { (catKey, catLabel) ->
                        FilterChip(
                            selected = selectedCategory == catKey,
                            onClick = {
                                selectedCategory = catKey
                                when (catKey) {
                                    "COURT_RENTAL" -> {
                                        title = "Monthly Pitch Rental & Referees"
                                        attachmentName = "Pitch_Rental_Signed_Invoice.pdf"
                                        attachmentUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800&auto=format&fit=crop&q=60"
                                    }
                                    "TOURNAMENT" -> {
                                        title = "Spring Championship League Entry Fee"
                                        attachmentName = "League_Tournament_Registration.pdf"
                                        attachmentUrl = "https://images.unsplash.com/photo-1450133064473-71024230f91b?w=800&auto=format&fit=crop&q=60"
                                    }
                                    "EQUIPMENT" -> {
                                        title = "Match Balls, Cones & Training Kits"
                                        attachmentName = "Sports_Store_Receipt_Official.jpg"
                                        attachmentUrl = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=800&auto=format&fit=crop&q=60"
                                    }
                                    "COACHING_REFS" -> {
                                        title = "Certified Referee Fees & Stipends"
                                        attachmentName = "Referees_Association_Stipend_Voucher.pdf"
                                        attachmentUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800&auto=format&fit=crop&q=60"
                                    }
                                }
                            },
                            label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Cost Item Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total Incurred Cost ($)") },
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

                // Attached Invoice / Screenshot Proof Info
                Text("Attached Invoice / Receipt Proof:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(attachmentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📎 Screenshot proof will be shared and viewable by all squad members in their invoice breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && title.isNotBlank()) {
                        onSubmit(
                            selectedTeamId,
                            title,
                            description,
                            amount,
                            selectedMonth,
                            selectedYear,
                            selectedCategory,
                            attachmentUrl,
                            attachmentType,
                            attachmentName
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("submit_create_budget_btn")
            ) {
                Text("Declare & Share")
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
