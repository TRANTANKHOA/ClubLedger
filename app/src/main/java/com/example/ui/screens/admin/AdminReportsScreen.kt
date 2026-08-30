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
import com.example.data.entity.AuditLog
import com.example.data.entity.User
import com.example.ui.components.CsvViewerDialog
import com.example.ui.components.StatKpiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminReportsScreen(
    viewModel: ClubViewModel,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val memberSummaries by viewModel.memberSummaries.collectAsState()
    val attendances by viewModel.allAttendances.collectAsState()
    val payments by viewModel.allPayments.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()

    var activeCsvExportTitle by remember { mutableStateOf<String?>(null) }
    var activeCsvExportContent by remember { mutableStateOf<String?>(null) }

    val totalBudgeted = budgets.sumOf { it.totalAmount }
    val totalCollected = payments.filter { it.status == "APPROVED" }.sumOf { it.amount }
    val totalOwing = memberSummaries.filter { it.balance < 0 }.sumOf { -it.balance }
    val totalSessions = attendances.count { it.status == "APPROVED" }
    val cloudSyncState by viewModel.cloudSyncState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }

    var clubIdInput by remember { mutableStateOf("thunder-sports-club") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Reports & Cloud Hub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Export accounting data, manage remote sync, and review audit logs.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Financial KPIs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatKpiCard(
                    title = "Total Invoiced",
                    value = "$${String.format(Locale.US, "%.2f", totalBudgeted)}",
                    subtitle = "${budgets.size} budget cycles",
                    icon = Icons.Default.ReceiptLong,
                    iconColor = SecondaryNavy,
                    modifier = Modifier.weight(1f)
                )

                StatKpiCard(
                    title = "Total Collected",
                    value = "$${String.format(Locale.US, "%.2f", totalCollected)}",
                    subtitle = "Deposited funds",
                    icon = Icons.Default.CheckCircle,
                    iconColor = SuccessGreen,
                    containerColor = SuccessContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Remote Multi-User & Firebase Cloud Sync Card
        item {
            Text(
                text = "Remote Multi-User & Cloud Sync",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryGreen)
                            Column {
                                Text("Firebase Real-Time Sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                val statusText = when (val s = cloudSyncState) {
                                    is com.example.data.remote.CloudSyncState.Disabled -> "Local Mode (Room DB Only)"
                                    is com.example.data.remote.CloudSyncState.Connecting -> "Connecting to Firestore..."
                                    is com.example.data.remote.CloudSyncState.Active -> "Live Connected (Club: ${s.clubId})"
                                    is com.example.data.remote.CloudSyncState.Syncing -> s.message
                                    is com.example.data.remote.CloudSyncState.Error -> "Offline: ${s.message}"
                                }
                                Text(statusText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }

                        Switch(
                            checked = cloudSyncState !is com.example.data.remote.CloudSyncState.Disabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    viewModel.enableCloudSync(clubIdInput)
                                } else {
                                    viewModel.disableCloudSync()
                                }
                            },
                            modifier = Modifier.testTag("cloud_sync_toggle")
                        )
                    }

                    OutlinedTextField(
                        value = clubIdInput,
                        onValueChange = { clubIdInput = it },
                        label = { Text("Shared Club Identifier / Channel") },
                        placeholder = { Text("e.g. thunder-sports-club") },
                        leadingIcon = { Icon(Icons.Default.GroupWork, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = { viewModel.syncAllToCloud() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sync_to_cloud_btn")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Local Records to Cloud Channel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = SurfaceBorder.copy(alpha = 0.5f))

                    Text(
                        text = "Cloud Account Authentication",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = SecondaryNavy
                    )

                    // Auth State Display
                    when (val auth = authState) {
                        is com.example.data.remote.AuthState.Authenticated -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SuccessGreen.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Authenticated via ${auth.provider}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SuccessGreen
                                        )
                                        Text(
                                            text = auth.user.email ?: auth.user.displayName ?: auth.user.uid,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.signOutCloud() },
                                        modifier = Modifier.testTag("sign_out_cloud_btn")
                                    ) {
                                        Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is com.example.data.remote.AuthState.Authenticating -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting to Auth Provider...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        else -> {
                            Text(
                                text = "Sign in to associate club ledger audits with your verified identity:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Social Auth Buttons (Google, Apple, Facebook)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.signInWithGoogle() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("google_auth_btn"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                activity?.let { viewModel.signInWithApple(it) }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("apple_auth_btn"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = SecondaryNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apple", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                activity?.let { viewModel.signInWithFacebook(it) }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("facebook_auth_btn"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1877F2))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Facebook", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // CSV Export Center
        item {
            Text(
                text = "Export Accounting Reports (CSV)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Download or copy ready-to-use CSV exports for spreadsheets, taxes, and treasurer audits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // Export Attendance
                    Button(
                        onClick = {
                            activeCsvExportTitle = "Club Attendance Ledger CSV"
                            activeCsvExportContent = viewModel.getAttendanceCsvString()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_attendance_csv_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Attendance Records CSV (${attendances.size} rows)")
                    }

                    // Export Payments
                    Button(
                        onClick = {
                            activeCsvExportTitle = "Club Payments Ledger CSV"
                            activeCsvExportContent = viewModel.getPaymentsCsvString()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_payments_csv_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Payments & Dues CSV (${payments.size} rows)")
                    }

                    // Export Balance Ledger
                    OutlinedButton(
                        onClick = {
                            activeCsvExportTitle = "Club Full Running Balance Ledger CSV"
                            activeCsvExportContent = viewModel.getLedgerCsvString()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_ledger_csv_btn")
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Running Balance Ledger CSV")
                    }
                }
            }
        }

        // Live Audit Log Stream
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Immutable Audit Trail",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${auditLogs.size} events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        if (auditLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No audit events recorded yet.", color = TextSecondary, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            items(auditLogs) { log ->
                AuditLogItemCard(log = log)
            }
        }
    }

    // CSV Viewer / Copy Dialog
    if (activeCsvExportTitle != null && activeCsvExportContent != null) {
        CsvViewerDialog(
            title = activeCsvExportTitle!!,
            csvContent = activeCsvExportContent!!,
            onDismiss = {
                activeCsvExportTitle = null
                activeCsvExportContent = null
            }
        )
    }
}

@Composable
fun AuditLogItemCard(log: AuditLog) {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.US)

    val (bg, iconColor, icon) = when {
        log.action.contains("APPROVED") -> Triple(SuccessContainer, SuccessGreen, Icons.Default.CheckCircle)
        log.action.contains("REJECTED") -> Triple(ErrorContainer, ErrorRed, Icons.Default.Cancel)
        log.action.contains("ALLOCATED") || log.action.contains("BUDGET") -> Triple(PrimaryContainer, PrimaryGreen, Icons.Default.Calculate)
        log.action.contains("MANUAL") -> Triple(GoldContainer, GoldAccent, Icons.Default.Tune)
        else -> Triple(SecondaryContainer, SecondaryNavy, Icons.Default.History)
    }

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
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.action.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = sdf.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
