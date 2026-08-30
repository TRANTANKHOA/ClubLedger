package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.InvoiceAllocation
import com.example.data.entity.User
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (bg, textColor, icon) = when (status.uppercase()) {
        "APPROVED", "ISSUED", "ACTIVE", "RESOLVED_CREDITED", "RESOLVED_EXPLAINED" -> Triple(SuccessContainer, SuccessGreen, Icons.Default.CheckCircle)
        "PENDING", "DRAFT", "OPEN" -> Triple(WarningContainer, WarningAmber, Icons.Default.Schedule)
        "REJECTED", "CANCELLED", "DISMISSED" -> Triple(ErrorContainer, ErrorRed, Icons.Default.Cancel)
        "INVOICED" -> Triple(InfoContainer, InfoBlue, Icons.Default.ReceiptLong)
        else -> Triple(SecondaryContainer, SecondaryNavy, Icons.Default.Info)
    }

    val displayStatus = when (status) {
        "RESOLVED_CREDITED" -> "RESOLVED (CREDITED)"
        "RESOLVED_EXPLAINED" -> "RESOLVED (SETTLED)"
        else -> status
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayStatus,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UserAvatar(user: User, size: Int = 40, modifier: Modifier = Modifier) {
    val initials = user.name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(user.avatarColorHex))
    ) {
        Text(
            text = initials.ifEmpty { "U" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.4).sp
        )
    }
}

@Composable
fun UserRoleSwitcherHeader(
    currentUser: User?,
    allUsers: List<User>,
    onSelectUser: (User) -> Unit,
    onRegisterClick: () -> Unit,
    onJoinTeamClick: (() -> Unit)? = null,
    cloudSyncState: com.example.data.remote.CloudSyncState? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = SecondaryNavy,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { expanded = true }
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("user_switcher_button")
            ) {
                if (currentUser != null) {
                    UserAvatar(user = currentUser, size = 28)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = currentUser.name,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentUser.role == "ADMIN") "Club Admin 👑" else "Team Member ⚽",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Account",
                        tint = Color.White
                    )
                } else {
                    Text("Select Profile", color = Color.White)
                }
            }

            // Quick Switcher Dropdown
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Text(
                    text = "SWITCH USER PROFILE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Divider()
                allUsers.forEach { user ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(user = user, size = 30)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        fontWeight = if (user.id == currentUser?.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${user.role} • ${user.email}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelectUser(user)
                            expanded = false
                        },
                        trailingIcon = {
                            if (user.id == currentUser?.id) {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = PrimaryGreen)
                            }
                        }
                    )
                }
                Divider()
                DropdownMenuItem(
                    text = { Text("+ Register New Member", color = PrimaryGreen, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        expanded = false
                        onRegisterClick()
                    },
                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryGreen) }
                )
                if (onJoinTeamClick != null) {
                    DropdownMenuItem(
                        text = { Text("🔗 Join Team with Invite Link / Code", color = InfoBlue, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            expanded = false
                            onJoinTeamClick()
                        },
                        leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = InfoBlue) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onJoinTeamClick != null && currentUser?.role != "ADMIN") {
                    FilledTonalButton(
                        onClick = onJoinTeamClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("topbar_join_team_btn")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Join Team", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (cloudSyncState is com.example.data.remote.CloudSyncState.Active) {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = "Cloud Connected", tint = SuccessContainer, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CLOUD", color = SuccessContainer, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Surface(
                    color = if (currentUser?.role == "ADMIN") GoldAccent.copy(alpha = 0.2f) else PrimaryGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (currentUser?.role == "ADMIN") "ADMIN MODE" else "MEMBER VIEW",
                        color = if (currentUser?.role == "ADMIN") GoldAccent else PrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color = SurfaceCard,
    iconColor: Color = PrimaryGreen,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun CostCalculationDialog(
    allocation: InvoiceAllocation?,
    totalBudget: Double,
    totalSessions: Int,
    onDismiss: () -> Unit
) {
    if (allocation == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("How My Cost Was Calculated", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "In ClubLedger, monthly team expenses are shared proportionally based only on verified sessions you attended.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = SecondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "PROPORTIONAL FORMULA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your Share = (Your Sessions ÷ Total Team Sessions) × Total Budget",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = SecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Team Budget:", color = TextSecondary)
                    Text("$${String.format(Locale.US, "%.2f", totalBudget)}", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Verified Team Sessions:", color = TextSecondary)
                    Text("$totalSessions sessions", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Your Approved Attendance:", color = TextSecondary)
                    Text("${allocation.approvedSessionsCount} sessions (${allocation.percentage}%)", fontWeight = FontWeight.Bold, color = PrimaryGreen)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Effective Rate per Session:", color = TextSecondary)
                    val rate = if (totalSessions > 0) totalBudget / totalSessions else 0.0
                    Text("$${String.format(Locale.US, "%.2f", rate)} / session", fontWeight = FontWeight.Medium)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Your Allocated Charge:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$${String.format(Locale.US, "%.2f", allocation.allocatedAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
fun CsvViewerDialog(
    title: String,
    csvContent: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Exported formatted CSV for club accounting & spreadsheets:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Text(
                        text = csvContent,
                        color = Color(0xFFF1F5F9),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                if (copied) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✓ Copied to clipboard!", color = SuccessGreen, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(csvContent))
                    copied = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy CSV")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
