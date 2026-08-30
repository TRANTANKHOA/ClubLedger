package com.example.ui.components

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.User
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuePaymentRequestDialog(
    members: List<User>,
    preSelectedUser: User? = null,
    onDismiss: () -> Unit,
    onSubmit: (userIds: List<Long>, title: String, amount: Double, memo: String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedToClipboard by remember { mutableStateOf(false) }

    val playingMembers = remember(members) { members.filter { it.role == "MEMBER" } }

    var selectedUserIds by remember {
        mutableStateOf(
            if (preSelectedUser != null) setOf(preSelectedUser.id)
            else playingMembers.map { it.id }.toSet()
        )
    }

    var selectedPreset by remember { mutableStateOf("Uniform & Kit") }
    var requestTitle by remember { mutableStateOf("Team Kit & Uniform Fee") }
    var amountInput by remember { mutableStateOf("45.00") }
    var memoInput by remember { mutableStateOf("Payable via Zelle/Venmo to club treasury by Friday.") }

    fun updatePreset(preset: String) {
        selectedPreset = preset
        when (preset) {
            "Uniform & Kit" -> {
                requestTitle = "Team Kit & Uniform Fee"
                amountInput = "45.00"
            }
            "Tournament" -> {
                requestTitle = "Regional Tournament Entry Fee"
                amountInput = "25.00"
            }
            "League Registration" -> {
                requestTitle = "Season League Registration Dues"
                amountInput = "35.00"
            }
            "Field Surcharge" -> {
                requestTitle = "Extra Pitch Lighting & Facility Fee"
                amountInput = "20.00"
            }
            "Custom" -> {
                requestTitle = "Custom Club Dues Request"
            }
        }
    }

    val formattedShareMessage = remember(requestTitle, amountInput, memoInput, selectedUserIds) {
        val count = selectedUserIds.size
        """
        📢 *ClubLedger Dues Request*
        ━━━━━━━━━━━━━━━━━━━━
        🏆 *Fee:* $requestTitle
        💵 *Amount:* $${amountInput.ifEmpty { "0.00" }} per player
        📝 *Notes:* $memoInput
        
        📲 Please submit your payment verification in the ClubLedger app after transferring via Zelle/Venmo!
        """.trimIndent()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCard,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WarningAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RequestQuote, contentDescription = null, tint = WarningAmber)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Issue Payment Request",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Post debit to member ledger & notify",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceBorder)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Fee Type Presets
                    item {
                        Text(
                            text = "1. Fee Category Preset",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val presets = listOf("Uniform & Kit", "Tournament", "League Registration", "Field Surcharge", "Custom")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.take(3).forEach { preset ->
                                val isSelected = preset == selectedPreset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { updatePreset(preset) },
                                    label = { Text(preset, style = MaterialTheme.typography.bodySmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SecondaryNavy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.drop(3).forEach { preset ->
                                val isSelected = preset == selectedPreset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { updatePreset(preset) },
                                    label = { Text(preset, style = MaterialTheme.typography.bodySmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SecondaryNavy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Title & Amount inputs
                    item {
                        Text(
                            text = "2. Request Details & Amount",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = requestTitle,
                            onValueChange = { requestTitle = it },
                            label = { Text("Request Title / Reason") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Amount Per Member ($)") },
                            leadingIcon = { Text("$", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = memoInput,
                            onValueChange = { memoInput = it },
                            label = { Text("Payment Memo / Instructions") },
                            placeholder = { Text("e.g., Pay via Zelle/Venmo to treasury@club.com...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )
                    }

                    // Shareable message preview
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = InfoContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💬 Group Chat Share Text",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InfoBlue
                                    )
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(formattedShareMessage))
                                            copiedToClipboard = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (copiedToClipboard) "Copied!" else "Copy", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(
                                    text = formattedShareMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Target Members Selection
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3. Target Members (${selectedUserIds.size}/${playingMembers.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryNavy
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { selectedUserIds = playingMembers.map { it.id }.toSet() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("All", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
                                }
                                TextButton(
                                    onClick = { selectedUserIds = emptySet() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                                }
                            }
                        }
                    }

                    items(playingMembers) { member ->
                        val isChecked = selectedUserIds.contains(member.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) WarningContainer.copy(alpha = 0.35f) else SurfaceLight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUserIds = if (isChecked) {
                                        selectedUserIds - member.id
                                    } else {
                                        selectedUserIds + member.id
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(user = member, size = 34)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = member.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedUserIds = if (checked) {
                                            selectedUserIds + member.id
                                        } else {
                                            selectedUserIds - member.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = WarningAmber)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
                val totalBatchAmount = parsedAmount * selectedUserIds.size

                // Submit Button
                Button(
                    onClick = {
                        if (selectedUserIds.isNotEmpty() && parsedAmount > 0.0) {
                            onSubmit(
                                selectedUserIds.toList(),
                                requestTitle.ifEmpty { "Dues Request" },
                                parsedAmount,
                                memoInput
                            )
                            onDismiss()
                        }
                    },
                    enabled = selectedUserIds.isNotEmpty() && parsedAmount > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_payment_request_btn")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Issue Request for $${String.format(Locale.US, "%.2f", totalBatchAmount)} (${selectedUserIds.size} Members)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
