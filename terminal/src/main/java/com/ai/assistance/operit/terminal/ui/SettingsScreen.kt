package com.ai.assistance.operit.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private object SettingsTheme {
    val backgroundColor = Color(0xFF11161C)
    val surfaceColor = Color(0xFF1B2530)
    val primaryColor = Color(0xFF5BA4FF)
    val textColor = Color(0xFFD7E3F4)
    val secondaryTextColor = Color(0xFF9FB3C8)
    val dangerColor = Color(0xFFC45555)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel =
        viewModel { SettingsViewModel(context.applicationContext as android.app.Application) }

    val cacheSize by viewModel.cacheSize.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val isCalculatingCache by viewModel.isCalculatingCache.collectAsState()
    val sshEnabled by viewModel.sshEnabled.collectAsState()
    val sshConfig by viewModel.sshConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(com.ai.assistance.operit.terminal.R.string.settings_title),
                        color = SettingsTheme.textColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SettingsTheme.surfaceColor)
            )
        },
        containerColor = SettingsTheme.backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsTheme.backgroundColor)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(
                title = context.getString(com.ai.assistance.operit.terminal.R.string.storage_management_title)
            ) {
                Text(
                    text = context.getString(
                        com.ai.assistance.operit.terminal.R.string.ubuntu_environment_size,
                        cacheSize
                    ),
                    color = SettingsTheme.secondaryTextColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.getCacheSize() },
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsTheme.primaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isCalculatingCache) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = context.getString(com.ai.assistance.operit.terminal.R.string.refresh_size),
                                color = Color.White
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.clearCache() },
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsTheme.dangerColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = context.getString(com.ai.assistance.operit.terminal.R.string.reset_environment),
                            color = Color.White
                        )
                    }
                }
            }

            SettingsCard(
                title = context.getString(com.ai.assistance.operit.terminal.R.string.project_address_title)
            ) {
                Text(text = updateStatus, color = SettingsTheme.secondaryTextColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.openGitHubRepo() },
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsTheme.primaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = context.getString(com.ai.assistance.operit.terminal.R.string.visit_project),
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsTheme.surfaceColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = context.getString(com.ai.assistance.operit.terminal.R.string.check_updates),
                            color = SettingsTheme.textColor
                        )
                    }
                }
            }

            SettingsCard(title = "SSH") {
                Text(
                    text = if (sshEnabled) "Enabled" else "Disabled",
                    color = SettingsTheme.secondaryTextColor
                )
                Text(
                    text = sshConfig?.host ?: "No SSH host configured",
                    color = SettingsTheme.secondaryTextColor,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.setSSHEnabled(!sshEnabled) },
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsTheme.primaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (sshEnabled) "Disable SSH" else "Enable SSH",
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsTheme.surfaceColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Back", color = SettingsTheme.textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SettingsTheme.surfaceColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = SettingsTheme.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            content()
        }
    }
}
