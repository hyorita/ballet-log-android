package com.hyorita.balletlog.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.BackupManager
import com.hyorita.balletlog.data.HealthConnectManager
import com.hyorita.balletlog.data.ProfilePreferences
import com.hyorita.balletlog.data.TermLanguage
import com.hyorita.balletlog.data.TermLanguagePreferences
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var instagramId by remember { mutableStateOf(ProfilePreferences.getInstagramId(context)) }
    var termLanguage by remember { mutableStateOf(TermLanguagePreferences.get(context)) }
    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isWorking = true
            statusMessage = try {
                BackupManager.exportToUri(context, uri)
                context.getString(R.string.settings_export_success)
            } catch (e: Throwable) {
                context.getString(R.string.settings_export_failed, e.message ?: "")
            }
            isWorking = false
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isWorking = true
            try {
                BackupManager.importFromUri(context, uri)
                showRestartDialog = true
            } catch (e: BackupManager.InvalidArchiveException) {
                statusMessage = context.getString(R.string.settings_invalid_archive)
            } catch (e: Throwable) {
                statusMessage = context.getString(R.string.settings_import_failed, e.message ?: "")
            }
            isWorking = false
        }
    }

    val onExportBackup: () -> Unit = {
        if (!isWorking) createDocLauncher.launch(BackupManager.suggestedFileName())
    }
    val onImportBackup: () -> Unit = {
        if (!isWorking) openDocLauncher.launch(arrayOf("*/*"))
    }
    val onClearStatus: () -> Unit = { statusMessage = null }

    // Auto-save on value change so leaving the screen always persists.
    LaunchedEffect(instagramId) {
        ProfilePreferences.setInstagramId(context, instagramId)
    }

    val versionName = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName} (${pi.longVersionCode})"
        }.getOrDefault("?")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.photolog_settings),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.done)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile
            item {
                SectionHeader(stringResource(R.string.settings_profile))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "@",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(2.dp))
                        BasicTextField(
                            value = instagramId,
                            onValueChange = { v ->
                                instagramId = v.removePrefix("@").trim()
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false
                            ),
                            decorationBox = { inner ->
                                if (instagramId.isEmpty()) {
                                    Text(
                                        stringResource(R.string.settings_username_placeholder),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
            }

            // Backup
            item {
                SectionHeader(stringResource(R.string.settings_backup))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Upload,
                            title = stringResource(R.string.settings_export_data),
                            enabled = !isWorking,
                            onClick = onExportBackup
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        SettingsRow(
                            icon = Icons.Default.Download,
                            title = stringResource(R.string.settings_import_data),
                            enabled = !isWorking,
                            onClick = onImportBackup
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_backup_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                if (isWorking) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            stringResource(R.string.settings_working),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Health Connect (1.8 auto-PhotoLog)
            item {
                val hcAvailable = remember { HealthConnectManager.isAvailable(context) }
                var hcGranted by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    hcGranted = HealthConnectManager.hasPermissions(context)
                }
                val permLauncher = rememberLauncherForActivityResult(
                    PermissionController.createRequestPermissionResultContract()
                ) {
                    scope.launch {
                        hcGranted = HealthConnectManager.hasPermissions(context)
                    }
                }
                SectionHeader(stringResource(R.string.settings_health_connect))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.MonitorHeart,
                            title = if (hcGranted)
                                stringResource(R.string.settings_health_connect_connected)
                            else
                                stringResource(R.string.settings_health_connect_connect),
                            enabled = hcAvailable && !hcGranted,
                            onClick = {
                                permLauncher.launch(HealthConnectManager.permissions)
                            }
                        )
                        if (hcAvailable) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            SettingsRow(
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                title = stringResource(R.string.settings_health_connect_open),
                                enabled = true,
                                onClick = {
                                    // Health Connect's settings activity. Falls back
                                    // to the Play Store entry for the Health Connect
                                    // companion app when the platform settings
                                    // intent isn't resolvable.
                                    val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                                    val resolved = intent.resolveActivity(context.packageManager) != null
                                    if (resolved) {
                                        context.startActivity(intent)
                                    } else {
                                        val store = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                        )
                                        runCatching { context.startActivity(store) }
                                    }
                                }
                            )
                        }
                    }
                }
                Text(
                    if (hcAvailable)
                        stringResource(R.string.settings_health_connect_footer)
                    else
                        stringResource(R.string.settings_health_connect_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Combination terms language
            item {
                SectionHeader(stringResource(R.string.settings_combo_terms))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val options = listOf(
                            TermLanguage.System to stringResource(R.string.term_language_system),
                            TermLanguage.English to stringResource(R.string.term_language_english),
                            TermLanguage.Korean to stringResource(R.string.term_language_korean)
                        )
                        options.forEachIndexed { index, (lang, label) ->
                            SegmentedButton(
                                selected = termLanguage == lang,
                                onClick = {
                                    termLanguage = lang
                                    TermLanguagePreferences.set(context, lang)
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.settings_combo_terms_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Share Ballet Log
            item {
                val shareSubject = stringResource(R.string.app_name)
                val shareBody = stringResource(R.string.settings_share_message) +
                    "\nhttps://play.google.com/store/apps/details?id=com.hyorita.balletlog"
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SettingsRow(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.settings_share_app),
                        enabled = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, shareSubject)
                            )
                        }
                    )
                }
                Text(
                    stringResource(R.string.settings_share_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // About
            item {
                SectionHeader(stringResource(R.string.settings_about))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        InfoRow(
                            label = stringResource(R.string.settings_version),
                            value = versionName
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        InfoRow(
                            label = stringResource(R.string.settings_developer),
                            value = "hyorita"
                        )
                    }
                }
            }
        }
    }

    statusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = onClearStatus,
            title = { Text(stringResource(R.string.settings_backup)) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = onClearStatus) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { /* require explicit OK */ },
            title = { Text(stringResource(R.string.settings_backup)) },
            text = { Text(stringResource(R.string.settings_import_success)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    // Forcefully restart the process so every ViewModel
                    // re-opens against the freshly restored DB.
                    android.os.Process.killProcess(android.os.Process.myPid())
                }) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 28.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
