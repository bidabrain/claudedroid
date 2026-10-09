package com.clawdroid.app.ui.settings

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.clawdroid.app.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.clawdroid.app.core.config.AppConfigManager
import com.clawdroid.app.core.skills.EditableSkill
import com.clawdroid.app.core.skills.SkillSettingsManager
import com.clawdroid.app.ui.components.GlassButton
import com.clawdroid.app.ui.components.GlassCard
import com.clawdroid.app.ui.components.GlassTextField
import com.clawdroid.app.ui.components.GlowText
import com.clawdroid.app.ui.theme.DeepBlack
import com.clawdroid.app.ui.theme.EmberOrange
import com.clawdroid.app.ui.theme.MutedGray
import com.clawdroid.app.ui.theme.SoftWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var skills by remember { mutableStateOf(SkillSettingsManager.list(context)) }
    var editing by remember { mutableStateOf<EditableSkill?>(null) }
    var editorText by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newSkillName by remember { mutableStateOf("") }
    var heartbeatEnabled by remember { mutableStateOf(AppConfigManager.heartbeatEnabled) }
    var heartbeatInterval by remember { mutableStateOf(AppConfigManager.heartbeatIntervalMin.toString()) }

    fun reload() {
        skills = SkillSettingsManager.list(context)
    }

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.config_skills_title), color = SoftWhite, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.config_back), tint = SoftWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.config_skills_create_cd), tint = SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlack),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlack)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlowText(stringResource(R.string.config_skills_settings_header), style = MaterialTheme.typography.titleLarge)
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.config_skills_heartbeat_title), color = SoftWhite, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.config_skills_heartbeat_desc, "HEARTBEAT.md"), color = MutedGray, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = heartbeatEnabled,
                            onCheckedChange = {
                                heartbeatEnabled = it
                                AppConfigManager.heartbeatEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmberOrange),
                        )
                    }
                    if (heartbeatEnabled) {
                        GlassTextField(
                            value = heartbeatInterval,
                            onValueChange = {
                                heartbeatInterval = it.filter(Char::isDigit).take(3)
                                AppConfigManager.heartbeatIntervalMin = heartbeatInterval.toIntOrNull()?.coerceIn(15, 120) ?: 15
                            },
                            placeholder = stringResource(R.string.config_skills_interval_minutes),
                        )
                    }
                }
            }

            if (skills.isEmpty()) {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.config_skills_empty_title), color = SoftWhite, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.config_skills_empty_desc), color = MutedGray)
                        GlassButton(onClick = { showCreateDialog = true }) {
                            Text(stringResource(R.string.config_skills_create), color = SoftWhite, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                skills.forEach { skill ->
                    SkillRow(
                        skill = skill,
                        onToggle = { enabled ->
                            SkillSettingsManager.setEnabled(context, skill.id, enabled)
                            reload()
                        },
                        onEdit = {
                            editing = skill
                            editorText = skill.content
                        },
                        onReset = {
                            SkillSettingsManager.resetBundled(context, skill.id)
                            reload()
                            Toast.makeText(context, context.getString(R.string.config_skills_reset_toast), Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.config_skills_create), color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = {
                GlassTextField(
                    value = newSkillName,
                    onValueChange = { newSkillName = it },
                    placeholder = "research-helper",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val skill = SkillSettingsManager.create(context, newSkillName)
                    reload()
                    editing = skill
                    editorText = skill.content
                    newSkillName = ""
                    showCreateDialog = false
                }) { Text(stringResource(R.string.config_create_caps), color = EmberOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.config_cancel_caps), color = SoftWhite) }
            },
            containerColor = DeepBlack,
        )
    }

    editing?.let { skill ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(skill.title, color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(skill.path, color = MutedGray, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    GlassTextField(
                        value = editorText,
                        onValueChange = { editorText = it },
                        singleLine = false,
                        maxLines = 18,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.height(320.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    SkillSettingsManager.save(context, skill.id, skill.path, editorText)
                    reload()
                    editing = null
                    Toast.makeText(context, context.getString(R.string.config_skills_saved_toast), Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.config_save_caps), color = EmberOrange) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(stringResource(R.string.config_cancel_caps), color = SoftWhite) }
            },
            containerColor = DeepBlack,
        )
    }
}

@Composable
private fun SkillRow(
    skill: EditableSkill,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onReset: () -> Unit,
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Extension, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.title, color = SoftWhite, fontWeight = FontWeight.Bold)
                Text(skill.status, color = MutedGray, style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.config_modified, DateFormat.format("MMM d, h:mm a", skill.lastModified).toString()),
                    color = MutedGray.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Switch(
                checked = skill.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = EmberOrange),
            )
            if (skill.bundled) {
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.config_skills_reset_cd),
                        tint = MutedGray,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
