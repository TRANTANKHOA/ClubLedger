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
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaiseDisputeDialog(
    initialCategory: String = "INVOICE_OVERCHARGE",
    initialReferenceType: String = "INVOICE_ALLOCATION",
    initialReferenceId: Long = 0,
    initialTitle: String = "",
    initialRequestedAmount: Double = 0.0,
    contextSummaryText: String = "",
    onDismiss: () -> Unit,
    onSubmit: (category: String, referenceType: String, referenceId: Long, title: String, description: String, requestedAmount: Double) -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf("") }
    var requestedAmountText by remember {
        mutableStateOf(if (initialRequestedAmount > 0) String.format(Locale.US, "%.2f", initialRequestedAmount) else "")
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "INVOICE_OVERCHARGE" to "Invoice / Session Overcharge",
        "ATTENDANCE_DISCREPANCY" to "Attendance Record Discrepancy",
        "PAYMENT_REJECTED" to "Rejected Payment Review",
        "DUES_REQUEST" to "Disputed Dues / Uniform Request",
        "GENERAL" to "General Billing Inquiry"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpCenter, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Raise Dispute / Inquiry", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (contextSummaryText.isNotBlank()) {
                    Surface(
                        color = SurfaceLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Regarding Item:", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text(contextSummaryText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.first == category }?.second ?: category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dispute Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { (catKey, catLabel) ->
                            DropdownMenuItem(
                                text = { Text(catLabel) },
                                onClick = {
                                    category = catKey
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Dispute Subject / Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Subject / Title *") },
                    placeholder = { Text("e.g. Marked absent for July 28 Match") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dispute_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Detailed explanation
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Explanation & Proof Details *") },
                    placeholder = { Text("Provide details, session dates, payment confirmations, or reason why you believe this charge/record is incorrect.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("dispute_desc_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                // Requested adjustment amount (optional)
                OutlinedTextField(
                    value = requestedAmountText,
                    onValueChange = { requestedAmountText = it },
                    label = { Text("Requested Credit / Refund Amount ($) [Optional]") },
                    placeholder = { Text("e.g. 50.00") },
                    leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = PrimaryGreen, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dispute_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Surface(
                    color = WarningContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This dispute will be queued for the team treasurer. All audit notes are saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reqAmt = requestedAmountText.toDoubleOrNull() ?: 0.0
                    onSubmit(
                        category,
                        initialReferenceType,
                        initialReferenceId,
                        title.ifBlank { "Dispute on $initialReferenceType" },
                        description.ifBlank { "Member submitted dispute." },
                        reqAmt
                    )
                    onDismiss()
                },
                enabled = title.isNotBlank() && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("submit_dispute_confirm_btn")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Submit Dispute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
