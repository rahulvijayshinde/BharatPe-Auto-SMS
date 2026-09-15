package com.autosms.bharatpe.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autosms.bharatpe.parser.NotificationParser
import com.autosms.bharatpe.ui.theme.ErrorRed
import com.autosms.bharatpe.ui.theme.SuccessGreen
import com.autosms.bharatpe.ui.viewmodel.MainViewModel
import com.autosms.bharatpe.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── Duplicate Protection ───────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Duplicate Protection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Prevents sending duplicate SMS for the same payment within a time window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Window: ${uiState.let { 30 }} minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Uses SHA-256 hashing + in-memory cache + database lookup",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // ─── Marathi Name Conversion ─────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Marathi Name Conversion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "मराठी नाव रूपांतरण",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Switch(
                            checked = uiState.marathiNameEnabled,
                            onCheckedChange = { viewModel.toggleMarathiName(it) }
                        )
                    }

                    Text(
                        "Automatically converts customer names from English into Marathi Devanagari script for destination SMS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Text(
                        "Examples:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "• RINKAL RAVINDR CHAURPAGAR → रिंकल रवींद्र चौरपगार\n" +
                        "• Miss DISHA SURESH RANDIVE → दिशा सुरेश रणदिवे\n" +
                        "• RAHUL VIJAY SHINDE → राहुल विजय शिंदे",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ─── BharatPe Package ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "BharatPe Package Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    var customPackage by remember { mutableStateOf(uiState.bharatPePackage) }

                    OutlinedTextField(
                        value = customPackage,
                        onValueChange = { customPackage = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("e.g. com.bharatpe.app") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.autoDetectPackage() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text(" Auto Detect")
                        }
                        Button(
                            onClick = { viewModel.selectBharatPePackage(customPackage) }
                        ) {
                            Text("Save")
                        }
                    }

                    if (uiState.bharatPeDetectedApps.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Detected apps:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        uiState.bharatPeDetectedApps.forEach { app ->
                            Text(
                                "• ${app.appLabel}: ${app.packageName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "How to find the package name manually:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "1. Open Settings → Apps → Manage Apps\n" +
                        "2. Find \"BharatPe\" or \"BharatPe for Business\"\n" +
                        "3. Tap on it → look for \"Package name\" or check the URL\n" +
                        "4. Enter the package name above",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ─── Parser Test ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Test Parser",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        "Paste a BharatPe notification text to test if it would trigger an SMS:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var testInput by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = testInput,
                        onValueChange = { testInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notification text") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { viewModel.testParser(testInput) },
                        enabled = testInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Test")
                    }

                    uiState.parserTestResult?.let { result ->
                        val isMatch = result.startsWith("✓")
                        Text(
                            result,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMatch) SuccessGreen else ErrorRed
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        "Built-in Test Cases:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    NotificationParser.getTestCases().forEach { (input, expected) ->
                        val result = NotificationParser.testParse(input)
                        val actual = if (result != null) "✓ Matched: ₹${result.formattedAmount}" else "✕ No match"
                        val isCorrect = (expected.startsWith("✓") == (result != null))

                        Text(
                            "${if (isCorrect) "✅" else "❌"} $actual — $expected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCorrect) SuccessGreen else ErrorRed
                        )
                    }
                }
            }

            // ─── Xiaomi/MIUI Setup Guide ────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Xiaomi / Redmi Setup Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        "Your Redmi Note 10S has aggressive battery management. " +
                        "Follow ALL steps below to ensure the app works reliably in the background:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SetupStep(
                        number = 1,
                        title = "Enable Autostart",
                        instructions = "Settings → Apps → Manage Apps → " +
                                "BharatPe Auto SMS → Autostart → Toggle ON"
                    )

                    SetupStep(
                        number = 2,
                        title = "Disable Battery Optimization",
                        instructions = "Settings → Apps → Manage Apps → " +
                                "BharatPe Auto SMS → Battery saver → Select \"No restrictions\""
                    )

                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    PermissionHelper.batteryOptimizationIntent(context)
                                )
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(
                                        PermissionHelper.appDetailSettingsIntent(context)
                                    )
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null)
                        Text(" Open Battery Settings")
                    }

                    SetupStep(
                        number = 3,
                        title = "Lock App in Recent Apps",
                        instructions = "Open Recent Apps → Find \"BharatPe Auto SMS\" → " +
                                "Long press the app card → Tap \"Lock\" (🔒)\n" +
                                "This prevents MIUI from killing the app."
                    )

                    SetupStep(
                        number = 4,
                        title = "Grant Notification Access",
                        instructions = "Settings → Notifications → Notification access → " +
                                "Toggle ON for \"BharatPe Auto SMS\"\n" +
                                "OR tap the button below:"
                    )

                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                PermissionHelper.notificationListenerSettingsIntent()
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Notification Access Settings")
                    }

                    SetupStep(
                        number = 5,
                        title = "MIUI Background Activity",
                        instructions = "Settings → Battery & performance → " +
                                "Background activity manager → " +
                                "Allow BharatPe Auto SMS to run in background"
                    )

                    SetupStep(
                        number = 6,
                        title = "Enable SMS Permission",
                        instructions = "Already handled by the app. If issues occur:\n" +
                                "Settings → Apps → Manage Apps → " +
                                "BharatPe Auto SMS → Permissions → SMS → Allow"
                    )

                    HorizontalDivider()

                    Text(
                        "⚠️ Important: These settings may reset after a system update. " +
                        "Re-check after any MIUI update.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )

                    // Try Autostart button
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = PermissionHelper.xiaomiAutostartIntent()
                                if (intent != null) {
                                    context.startActivity(intent)
                                }
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(
                                        PermissionHelper.appDetailSettingsIntent(context)
                                    )
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Try Opening Autostart Settings")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SetupStep(number: Int, title: String, instructions: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Step $number: $title",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            instructions,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
