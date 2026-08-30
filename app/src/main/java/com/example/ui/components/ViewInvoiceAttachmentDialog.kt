package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.entity.Invoice
import com.example.data.entity.Team
import com.example.data.entity.TeamBudget
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ViewInvoiceAttachmentDialog(
    invoice: Invoice? = null,
    budget: TeamBudget? = null,
    team: Team?,
    onDismiss: () -> Unit,
    onRaiseDispute: (() -> Unit)? = null
) {
    val title = invoice?.title ?: budget?.title ?: "Cost Item & Invoice"
    val totalAmount = invoice?.totalAmount ?: budget?.totalAmount ?: 0.0
    val periodMonth = invoice?.periodMonth ?: budget?.periodMonth ?: 1
    val periodYear = invoice?.periodYear ?: budget?.periodYear ?: 2026
    val category = invoice?.category ?: budget?.category ?: "COURT_RENTAL"
    val attachmentUrl = invoice?.attachmentUrl ?: budget?.attachmentUrl
    val attachmentName = invoice?.attachmentName ?: budget?.attachmentName ?: "Invoice_Receipt_Verified.pdf"
    val attachmentType = invoice?.attachmentType ?: budget?.attachmentType ?: "RECEIPT_IMAGE"
    val invoiceNumber = invoice?.invoiceNumber ?: "BDG-$periodYear-$periodMonth-${team?.name?.take(3)?.uppercase() ?: "CLM"}"
    val verifiedSessions = invoice?.totalApprovedSessions ?: 0
    val costPerSession = invoice?.costPerSession ?: 0.0

    val clipboardManager = LocalClipboardManager.current
    var copiedToClipboard by remember { mutableStateOf(false) }

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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryDark)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Verified Invoice Proof",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = invoiceNumber,
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category & Amount Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = when (category) {
                                        "COURT_RENTAL" -> PrimaryContainer
                                        "TOURNAMENT" -> WarningContainer
                                        "EQUIPMENT" -> InfoContainer
                                        "COACHING_REFS" -> SecondaryContainer
                                        else -> Color(0xFFF1F5F9)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (category) {
                                                "COURT_RENTAL" -> "🏟️ Court / Pitch Rental"
                                                "TOURNAMENT" -> "🏆 Tournament Entry"
                                                "EQUIPMENT" -> "⚽ Equipment & Gear"
                                                "COACHING_REFS" -> "👨‍🏫 Coaching & Referees"
                                                "TRANSPORT" -> "🚌 Transport & Travel"
                                                else -> "📦 Operating Cost"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = when (category) {
                                                "COURT_RENTAL" -> PrimaryDark
                                                "TOURNAMENT" -> WarningAmber
                                                "EQUIPMENT" -> InfoBlue
                                                else -> TextPrimary
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", totalAmount)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Team: ${team?.name ?: "Club"} • Period: ${getMonthName(periodMonth)} $periodYear",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Attached Document / Receipt Preview
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (attachmentType == "PDF_INVOICE") Icons.Default.PictureAsPdf else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = if (attachmentType == "PDF_INVOICE") Color(0xFFDC2626) else PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = attachmentName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Surface(
                                    color = Color(0xFFECFDF5),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "VERIFIED PROOF",
                                        color = PrimaryGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Image / PDF Screenshot Box
                            if (!attachmentUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = attachmentUrl,
                                        contentDescription = "Invoice proof screenshot",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Electronic Receipt Attached", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(attachmentName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Declared & verified by Team Administration. All squad members have transparent auditing access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    // Calculation Breakdown & Math
                    if (verifiedSessions > 0) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "MATHEMATICAL ALLOCATION BREAKDOWN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Verified Squad Sessions:", style = MaterialTheme.typography.bodyMedium, color = PrimaryDark)
                                    Text("$verifiedSessions sessions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Calculated Cost Per Session:", style = MaterialTheme.typography.bodyMedium, color = PrimaryDark)
                                    Text("$${String.format(Locale.US, "%.2f", costPerSession)} / session", fontWeight = FontWeight.Bold, color = PrimaryGreen, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Your individual charge = (Your attended sessions ÷ $verifiedSessions) × $${String.format(Locale.US, "%.2f", totalAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareSummary = "🧾 ClubLedger Cost Item\n" +
                                    "Invoice: $invoiceNumber\n" +
                                    "Title: $title\n" +
                                    "Category: $category\n" +
                                    "Total Incurred: $$totalAmount\n" +
                                    "Period: ${getMonthName(periodMonth)} $periodYear\n" +
                                    "Attachment: $attachmentName\n" +
                                    "Verified on ClubLedger"
                            clipboardManager.setText(AnnotatedString(shareSummary))
                            copiedToClipboard = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copiedToClipboard) "Copied!" else "Share Proof")
                    }

                    if (onRaiseDispute != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onRaiseDispute()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispute Item")
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

private fun getMonthName(monthNumber: Int): String {
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
