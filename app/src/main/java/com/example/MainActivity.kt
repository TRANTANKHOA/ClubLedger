package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.User
import com.example.ui.components.JoinTeamWithLinkDialog
import com.example.ui.components.UserRoleSwitcherHeader
import com.example.ui.screens.admin.*
import com.example.ui.screens.member.*
import com.example.ui.theme.ClubLedgerTheme
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryNavy
import com.example.ui.viewmodel.ClubViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // Member routes
    object MemberDashboard : Screen("member_dashboard", "Dashboard", Icons.Default.Dashboard)
    object MemberAttendance : Screen("member_attendance", "Attendance", Icons.Default.SportsScore)
    object MemberPayments : Screen("member_payments", "Payments", Icons.Default.Payment)
    object MemberInvoices : Screen("member_invoices", "Invoices", Icons.Default.ReceiptLong)

    // Admin routes
    object AdminDashboard : Screen("admin_dashboard", "Overview", Icons.Default.Dashboard)
    object AdminApprovals : Screen("admin_approvals", "Approvals", Icons.Default.FactCheck)
    object AdminBudgets : Screen("admin_budgets", "Budgets", Icons.Default.AccountBalanceWallet)
    object AdminMembers : Screen("admin_members", "Members", Icons.Default.People)
    object AdminReports : Screen("admin_reports", "Audit & CSV", Icons.Default.Assessment)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ClubViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClubLedgerTheme {
                ClubLedgerApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubLedgerApp(viewModel: ClubViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val teams by viewModel.allTeams.collectAsState()
    val pendingAttendances by viewModel.pendingAttendances.collectAsState()
    val pendingPayments by viewModel.pendingPayments.collectAsState()
    val pendingJoinRequests by viewModel.pendingJoinRequests.collectAsState()
    val openDisputes by viewModel.openDisputes.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val cloudSyncState by viewModel.cloudSyncState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val totalPendingApprovals = pendingAttendances.size + pendingPayments.size + pendingJoinRequests.size + openDisputes.size

    var currentScreen by remember { mutableStateOf<Screen>(Screen.MemberDashboard) }
    var showQuickAttendanceDialog by remember { mutableStateOf(false) }
    var showQuickPaymentDialog by remember { mutableStateOf(false) }
    var showRegisterMemberDialog by remember { mutableStateOf(false) }
    var showJoinTeamDialog by remember { mutableStateOf(false) }

    // Sync default screen when user changes role
    LaunchedEffect(currentUser?.role) {
        if (currentUser?.role == "ADMIN") {
            if (currentScreen is Screen.MemberDashboard || currentScreen is Screen.MemberAttendance ||
                currentScreen is Screen.MemberPayments || currentScreen is Screen.MemberInvoices
            ) {
                currentScreen = Screen.AdminDashboard
            }
        } else {
            if (currentScreen is Screen.AdminDashboard || currentScreen is Screen.AdminApprovals ||
                currentScreen is Screen.AdminBudgets || currentScreen is Screen.AdminMembers ||
                currentScreen is Screen.AdminReports
            ) {
                currentScreen = Screen.MemberDashboard
            }
        }
    }

    // Snackbar notifications
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val memberNavItems = listOf(
        Screen.MemberDashboard,
        Screen.MemberAttendance,
        Screen.MemberPayments,
        Screen.MemberInvoices
    )

    val adminNavItems = listOf(
        Screen.AdminDashboard,
        Screen.AdminApprovals,
        Screen.AdminBudgets,
        Screen.AdminMembers,
        Screen.AdminReports
    )

    val activeNavItems = if (currentUser?.role == "ADMIN") adminNavItems else memberNavItems

    Scaffold(
        topBar = {
            UserRoleSwitcherHeader(
                currentUser = currentUser,
                allUsers = allUsers,
                onSelectUser = { selectedUser ->
                    viewModel.switchUser(selectedUser)
                },
                onRegisterClick = {
                    showRegisterMemberDialog = true
                },
                onJoinTeamClick = {
                    showJoinTeamDialog = true
                },
                cloudSyncState = cloudSyncState,
                getUserRoleBadge = { user -> viewModel.getUserRoleBadge(user) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                activeNavItems.forEach { screen ->
                    val selected = currentScreen.route == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (screen == Screen.AdminApprovals && totalPendingApprovals > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("$totalPendingApprovals")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryGreen,
                            selectedTextColor = PrimaryGreen,
                            indicatorColor = Color(0xFFE0F2F1)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val user = currentUser
            if (user != null) {
                when (currentScreen) {
                    // Member Screens
                    Screen.MemberDashboard -> {
                        MemberDashboardScreen(
                            viewModel = viewModel,
                            currentUser = user,
                            onNavigateToAttendance = { currentScreen = Screen.MemberAttendance },
                            onNavigateToPayments = { currentScreen = Screen.MemberPayments },
                            onNavigateToInvoices = { currentScreen = Screen.MemberInvoices },
                            onOpenSubmitAttendance = { showQuickAttendanceDialog = true },
                            onOpenSubmitPayment = { showQuickPaymentDialog = true }
                        )
                    }
                    Screen.MemberAttendance -> {
                        MemberAttendanceScreen(
                            viewModel = viewModel,
                            currentUser = user
                        )
                    }
                    Screen.MemberPayments -> {
                        MemberPaymentsScreen(
                            viewModel = viewModel,
                            currentUser = user
                        )
                    }
                    Screen.MemberInvoices -> {
                        MemberInvoicesScreen(
                            viewModel = viewModel,
                            currentUser = user
                        )
                    }

                    // Admin Screens
                    Screen.AdminDashboard -> {
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            currentUser = user,
                            onNavigateToApprovals = { currentScreen = Screen.AdminApprovals },
                            onNavigateToBudgets = { currentScreen = Screen.AdminBudgets },
                            onNavigateToMembers = { currentScreen = Screen.AdminMembers },
                            onNavigateToReports = { currentScreen = Screen.AdminReports }
                        )
                    }
                    Screen.AdminApprovals -> {
                        AdminApprovalsScreen(
                            viewModel = viewModel,
                            currentUser = user
                        )
                    }
                    Screen.AdminBudgets -> {
                        AdminBudgetsScreen(
                            viewModel = viewModel,
                            currentUser = user
                        )
                    }
                    Screen.AdminMembers -> {
                        AdminMembersScreen(
                            viewModel = viewModel,
                            currentUser = user
                        )
                    }
                    Screen.AdminReports -> {
                        AdminReportsScreen(
                            viewModel = viewModel,
                            currentUser = user
                        )
                    }
                }
            }
        }
    }

    // Global dialogs
    if (showQuickAttendanceDialog) {
        SubmitAttendanceDialog(
            teams = teams,
            onDismiss = { showQuickAttendanceDialog = false },
            onSubmit = { teamId, dateMillis, sessionType, notes ->
                viewModel.submitAttendance(teamId, dateMillis, sessionType, notes)
                showQuickAttendanceDialog = false
            }
        )
    }

    if (showQuickPaymentDialog) {
        val currentBal = viewModel.currentUserBalance.collectAsState().value
        SubmitPaymentDialog(
            suggestedAmount = if (currentBal < 0) -currentBal else 50.0,
            onDismiss = { showQuickPaymentDialog = false },
            onSubmit = { amount, dateMillis, method, refNote, receiptNote ->
                viewModel.submitPayment(amount, dateMillis, method, refNote, receiptNote)
                showQuickPaymentDialog = false
            }
        )
    }

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

    if (showJoinTeamDialog) {
        JoinTeamWithLinkDialog(
            viewModel = viewModel,
            currentUser = currentUser,
            onDismiss = { showJoinTeamDialog = false }
        )
    }
}
