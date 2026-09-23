package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.Team
import com.example.data.entity.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClubViewModel
import com.example.util.RateLimitResult
import com.example.util.SecurityDefenseHelper
import kotlinx.coroutines.launch

@Composable
fun JoinTeamWithLinkDialog(
    viewModel: ClubViewModel,
    currentUser: User?,
    prefilledCode: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inviteInput by remember { mutableStateOf(prefilledCode) }
    var applicantName by remember { mutableStateOf(currentUser?.name ?: "") }
    var applicantEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    var applicantPhone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var introMessage by remember { mutableStateOf("") }

    var isSearchingTeam by remember { mutableStateOf(false) }
    var matchedTeam by remember { mutableStateOf<Team?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    // Automatically search team when input changes with rate-limiting and sanitization
    LaunchedEffect(inviteInput) {
        if (inviteInput.isNotBlank()) {
            val rateLimit = SecurityDefenseHelper.checkRateLimit("join_team_lookup")
            when (rateLimit) {
                is RateLimitResult.Throttled -> {
                    submitError = "Too many lookups. Please retry in ${rateLimit.retryAfterSeconds}s."
                    matchedTeam = null
                }
                is RateLimitResult.Allowed -> {
                    isSearchingTeam = true
                    val sanitized = SecurityDefenseHelper.sanitizeInviteCode(inviteInput)
                    matchedTeam = viewModel.previewTeamForCode(if (sanitized.isNotBlank()) sanitized else inviteInput)
                    isSearchingTeam = false
                }
            }
        } else {
            matchedTeam = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCard,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
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
                                .background(SecondaryNavy.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = SecondaryNavy, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Join Team with Link",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Submit membership request to owner",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Invite Link or Code Input
                OutlinedTextField(
                    value = inviteInput,
                    onValueChange = {
                        inviteInput = it
                        submitError = null
                    },
                    label = { Text("Team Join Link or Invite Code") },
                    placeholder = { Text("e.g. RIVERSIDE-26 or https://clubledger.app/join?team=...") },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryGreen)
                    },
                    trailingIcon = {
                        if (isSearchingTeam) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_link_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live Matched Team Card
                if (matchedTeam != null) {
                    Surface(
                        color = SuccessContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = matchedTeam!!.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${matchedTeam!!.sportType} • Code: ${matchedTeam!!.inviteCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else if (inviteInput.isNotBlank() && !isSearchingTeam) {
                    Surface(
                        color = WarningContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Code/Link not recognized yet. Try e.g. RIVERSIDE-26",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Applicant Information
                Text(
                    text = "Your Applicant Details",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = applicantName,
                    onValueChange = { applicantName = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("applicant_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = applicantEmail,
                    onValueChange = { applicantEmail = it },
                    label = { Text("Email Address *") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("applicant_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = applicantPhone,
                    onValueChange = { applicantPhone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 (555) 000-0000") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("applicant_phone_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = introMessage,
                    onValueChange = { introMessage = it },
                    label = { Text("Intro Note for Team Owner") },
                    placeholder = { Text("e.g. Played soccer in college, midfield position") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("applicant_message_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (submitError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = submitError!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inviteInput.isBlank()) {
                                submitError = "Please enter an invite link or team code."
                                return@Button
                            }
                            if (applicantName.isBlank()) {
                                submitError = "Please enter your full name."
                                return@Button
                            }
                            if (applicantEmail.isBlank()) {
                                submitError = "Please enter your email address."
                                return@Button
                            }

                            isSubmitting = true
                            viewModel.submitJoinRequest(
                                teamCodeOrLink = inviteInput,
                                name = applicantName,
                                email = applicantEmail,
                                phone = applicantPhone,
                                message = introMessage
                            ) { success, message ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    submitError = message
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_join_request_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submitting...")
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Join Request")
                        }
                    }
                }
            }
        }
    }
}
