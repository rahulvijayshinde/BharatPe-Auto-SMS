package com.autosms.bharatpe.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autosms.bharatpe.ui.theme.ErrorRed
import com.autosms.bharatpe.ui.theme.SuccessGreen
import com.autosms.bharatpe.ui.theme.WarningAmber
import com.autosms.bharatpe.ui.viewmodel.MainViewModel
import com.autosms.bharatpe.util.PermissionHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lastTransaction by viewModel.lastTransaction.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Permission launchers
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshState() }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshState() }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshState() }

    // Show test result as snackbar
    LaunchedEffect(uiState.lastTestResult) {
        uiState.lastTestResult?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearTestResult()
            }
        }
    }

    // Refresh state when screen is shown
    LaunchedEffect(Unit) {
        viewModel.refreshState()
        // Auto-detect BharatPe package on first launch
        if (uiState.bharatPePackage.isBlank()) {
            viewModel.autoDetectPackage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "BharatPe Auto SMS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (uiState.automationEnabled) "Active" else "Inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.automationEnabled) SuccessGreen
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── Automation Toggle Card ─────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.automationEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Automation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (uiState.automationEnabled) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = if (uiState.automationEnabled) "ON" else "OFF",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            if (uiState.automationEnabled)
                                "Automatic SMS: Enabled"
                            else "Automatic SMS: Disabled (Paused)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.automationEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.automationEnabled,
                        onCheckedChange = { viewModel.toggleAutomation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // ─── Permission Status Card ─────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    PermissionRow(
                        label = "Notification Access",
                        granted = uiState.notificationAccessGranted,
                        onRequest = {
                            context.startActivity(
                                PermissionHelper.notificationListenerSettingsIntent()
                            )
                        }
                    )

                    PermissionRow(
                        label = "SMS Permission",
                        granted = uiState.smsPermissionGranted,
                        onRequest = {
                            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                        }
                    )

                    PermissionRow(
                        label = "Battery Unrestricted",
                        granted = uiState.batteryOptimizationDisabled,
                        onRequest = {
                            context.startActivity(
                                PermissionHelper.batteryOptimizationIntent(context)
                            )
                        }
                    )
                }
            }

            // ─── Recipient Number Card ──────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Recipient Number",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "JioBharat phone number to receive payment SMS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var numberInput by remember { mutableStateOf(uiState.recipientNumber) }
                    LaunchedEffect(uiState.recipientNumber) {
                        numberInput = uiState.recipientNumber
                    }

                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = {
                            numberInput = it
                            viewModel.updateRecipientNumber(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("+91 XXXXXXXXXX") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ─── SMS Template Card ──────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "SMS Template",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    var templateInput by remember { mutableStateOf(uiState.smsTemplate) }
                    LaunchedEffect(uiState.smsTemplate) {
                        templateInput = uiState.smsTemplate
                    }

                    OutlinedTextField(
                        value = templateInput,
                        onValueChange = {
                            templateInput = it
                            viewModel.updateSmsTemplate(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Use {amount} as placeholder") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        "Preview: ${templateInput.replace("{amount}", "250")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ─── SIM Selection Card ─────────────────────────────────
            if (uiState.availableSims.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "SMS SIM Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Default option
                        SimOption(
                            label = "Default (System)",
                            selected = uiState.selectedSimSlot == -1,
                            onClick = { viewModel.selectSim(-1) }
                        )

                        // SIM options
                        uiState.availableSims.forEach { sim ->
                            SimOption(
                                label = "${sim.displayName} (${sim.carrierName})",
                                selected = uiState.selectedSimSlot == sim.slotIndex,
                                onClick = { viewModel.selectSim(sim.slotIndex) }
                            )
                        }
                    }
                }
            } else if (!uiState.phoneStatePermissionGranted) {
                OutlinedButton(
                    onClick = {
                        phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Phone Permission for SIM Selection")
                }
            }

            // ─── BharatPe Package Card ──────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "BharatPe App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (uiState.bharatPePackage.isNotBlank()) {
                        Text(
                            "Monitoring: ${uiState.bharatPePackage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SuccessGreen
                        )
                    } else {
                        Text(
                            "Not detected — please install BharatPe or tap Detect",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorRed
                        )
                    }

                    if (uiState.bharatPeDetectedApps.isNotEmpty()) {
                        Text(
                            "Found: ${uiState.bharatPeDetectedApps.joinToString { "${it.appLabel} (${it.packageName})" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.autoDetectPackage() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Detect BharatPe Package")
                    }
                }
            }

            // ─── Last Payment Card ──────────────────────────────────
            lastTransaction?.let { txn ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (txn.smsSent)
                            MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Last Payment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "₹${txn.amount}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val statusColor by animateColorAsState(
                                if (txn.smsSent) SuccessGreen else ErrorRed,
                                label = "statusColor"
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(statusColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (txn.smsSent) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.surface
                                )
                            }
                        }

                        Text(
                            if (txn.smsSent) "SMS sent automatically ✓"
                            else "SMS ${txn.smsError ?: "Failed"} ✕",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (txn.smsSent) SuccessGreen else ErrorRed
                        )

                        if (txn.senderName.isNotBlank()) {
                            Text(
                                "From: ${txn.senderName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            formatTimestamp(txn.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ─── Action Buttons ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.sendTestSms() },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.smsPermissionGranted && uiState.recipientNumber.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Test SMS")
                }

                OutlinedButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("History")
                }
            }

            // ─── Request All Permissions ────────────────────────────
            if (!uiState.smsPermissionGranted || !uiState.phoneStatePermissionGranted) {
                Button(
                    onClick = {
                        val perms = mutableListOf<String>()
                        if (!uiState.smsPermissionGranted) perms.add(Manifest.permission.SEND_SMS)
                        if (!uiState.phoneStatePermissionGranted) perms.add(Manifest.permission.READ_PHONE_STATE)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        multiPermissionLauncher.launch(perms.toTypedArray())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarningAmber
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Required Permissions", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (granted) SuccessGreen else ErrorRed)
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }

        if (!granted) {
            OutlinedButton(
                onClick = onRequest,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Text(
                "Granted",
                style = MaterialTheme.typography.labelSmall,
                color = SuccessGreen
            )
        }
    }
}

@Composable
private fun SimOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary)
                )
            }
        }

        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        )

        if (!selected) {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Select", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
