package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.Dispute
import com.example.data.entity.User
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResolveDisputeDialog(
    dispute: Dispute,
    member: User?,
    onDismiss: () -> Unit,
    onResolve: (action: String, notes: String, creditAmount: Double) -> Unit
) {
    var selectedAction by remember {
        mutableStateOf(if (dispute.requestedAdjustmentAmount > 0.0) "CREDIT_AND_RESOLVE" else "EXPLAIN_AND_RESOLVE")
    }
    var notes by remember { mutableStateOf("") }
    var creditAmountText by remember {
        mutableStateOf(if (dispute.requestedAdjustmentAmount > 0.0) String.format(Locale.US, "%.2f", dispute.requestedAdjustmentAmount) else "0.00")
    }

    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = SecondaryNavy, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resolve Dispute #${dispute.id}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Member Dispute Details Card
                Surface(
                    color = SurfaceLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = member?.name ?: "Member",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = sdf.format(Date(dispute.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dispute.title,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dispute.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        if (dispute.requestedAdjustmentAmount > 0.0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Requested Credit: $${String.format(Locale.US, "%.2f", dispute.requestedAdjustmentAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Text("Choose Resolution Decision:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                // Decision Radio / Buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedAction == "CREDIT_AND_RESOLVE") SuccessContainer.copy(alpha = 0.6f) else SurfaceLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAction == "CREDIT_AND_RESOLVE",
                                onClick = { selectedAction = "CREDIT_AND_RESOLVE" }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Accept & Credit Member Balance", fontWeight = FontWeight.Bold, color = SuccessGreen)
                                Text("Posts an offsetting ledger credit to member's account.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedAction == "EXPLAIN_AND_RESOLVE") InfoContainer.copy(alpha = 0.6f) else SurfaceLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAction == "EXPLAIN_AND_RESOLVE",
                                onClick = { selectedAction = "EXPLAIN_AND_RESOLVE" }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Explain & Settle (No Balance Change)", fontWeight = FontWeight.Bold, color = InfoBlue)
                                Text("Record was valid or mutually agreed upon without balance credit.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedAction == "DISMISS") ErrorContainer.copy(alpha = 0.4f) else SurfaceLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAction == "DISMISS",
                                onClick = { selectedAction = "DISMISS" }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Dismiss / Reject Dispute", fontWeight = FontWeight.Bold, color = ErrorRed)
                                Text("Dispute claim is invalid or duplicate.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                // If Credit Selected, show Credit Amount Input
                if (selectedAction == "CREDIT_AND_RESOLVE") {
                    OutlinedTextField(
                        value = creditAmountText,
                        onValueChange = { creditAmountText = it },
                        label = { Text("Credit Amount to Apply ($)") },
                        leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = PrimaryGreen, modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resolve_credit_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Official Treasurer Resolution Note
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Official Treasurer Resolution Notes *") },
                    placeholder = { Text("e.g. Verified attendance logs with coach; applied $25.00 credit to balance.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("resolve_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = creditAmountText.toDoubleOrNull() ?: 0.0
                    onResolve(
                        selectedAction,
                        notes.ifBlank { "Dispute resolved by treasurer" },
                        if (selectedAction == "CREDIT_AND_RESOLVE") amt else 0.0
                    )
                    onDismiss()
                },
                enabled = notes.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedAction) {
                        "CREDIT_AND_RESOLVE" -> SuccessGreen
                        "EXPLAIN_AND_RESOLVE" -> SecondaryNavy
                        else -> ErrorRed
                    }
                ),
                modifier = Modifier.testTag("resolve_dispute_confirm_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm Resolution")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
