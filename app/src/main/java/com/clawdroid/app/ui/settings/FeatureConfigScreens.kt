package com.clawdroid.app.ui.settings

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.clawdroid.app.R
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.clawdroid.app.core.config.AppConfigManager
import com.clawdroid.app.ui.components.AnimatedPresetCard
import com.clawdroid.app.ui.components.ChannelConnectionStatus
import com.clawdroid.app.ui.components.ChannelStatusCard
import com.clawdroid.app.ui.components.GlassButton
import com.clawdroid.app.ui.components.GlassCard
import com.clawdroid.app.ui.components.GlassTextField
import com.clawdroid.app.ui.components.MCPStatusRow
import com.clawdroid.app.ui.components.MCPServerInfo
import com.clawdroid.app.ui.components.PresetBadge
import com.clawdroid.app.ui.components.PresetStatus
import com.clawdroid.app.ui.components.ServerStatus
import com.clawdroid.app.ui.components.SetupWizardScaffold
import com.clawdroid.app.ui.components.WizardActionRow
import com.clawdroid.app.ui.components.WizardStep
import com.clawdroid.app.ui.theme.ActivePurple
import com.clawdroid.app.ui.theme.DeepBlack
import com.clawdroid.app.ui.theme.EmberOrange
import com.clawdroid.app.ui.theme.GlassBorderDim
import com.clawdroid.app.ui.theme.GlassFill
import com.clawdroid.app.ui.theme.GlassFillStrong
import com.clawdroid.app.ui.theme.MutedGray
import com.clawdroid.app.ui.theme.NeonBlue
import com.clawdroid.app.ui.theme.NeonCyan
import com.clawdroid.app.ui.theme.SoftWhite

@Composable
fun AudioConfigScreen(onBack: () -> Unit) {
    var ttsEngine by remember { mutableStateOf(AppConfigManager.ttsEngine) }
    var ttsVoice by remember { mutableStateOf(AppConfigManager.ttsVoice) }
    var ttsSpeed by remember { mutableStateOf(AppConfigManager.ttsSpeed) }
    var openaiKey by remember { mutableStateOf(AppConfigManager.openaiTtsApiKey) }
    var elevenlabsKey by remember { mutableStateOf(AppConfigManager.elevenlabsApiKey) }
    var deepgramKey by remember { mutableStateOf(AppConfigManager.deepgramApiKey) }
    var dynamicThinking by remember { mutableStateOf(AppConfigManager.dynamicThinkingEnabled) }
    var emojiTone by remember { mutableStateOf(AppConfigManager.emojiToneEnabled) }
    var piperEnabled by remember { mutableStateOf(AppConfigManager.mcpEnabled) }

    ConfigScaffold(stringResource(R.string.mcp_audio_title), Icons.Outlined.Headphones, onBack) {
        InfoCard(
            title = stringResource(R.string.mcp_voice_runtime),
            body = stringResource(R.string.mcp_voice_runtime_body)
        )

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_tts_engine))

                ConfigChoice("Android TTS", stringResource(R.string.mcp_tts_android_desc), ttsEngine == "device") { ttsEngine = "device" }
                ConfigChoice("OpenAI TTS", stringResource(R.string.mcp_tts_openai_desc), ttsEngine == "openai") { ttsEngine = "openai" }
                ConfigChoice("ElevenLabs", stringResource(R.string.mcp_tts_elevenlabs_desc), ttsEngine == "elevenlabs") { ttsEngine = "elevenlabs" }
                ConfigChoice("Deepgram", stringResource(R.string.mcp_tts_deepgram_desc), ttsEngine == "deepgram") { ttsEngine = "deepgram" }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_voice_details))
                GlassTextField(
                    value = ttsVoice,
                    onValueChange = { ttsVoice = it },
                    placeholder = stringResource(R.string.mcp_voice_id_placeholder),
                )
                Text(
                    stringResource(R.string.mcp_speech_speed, String.format("%.1fx", ttsSpeed)),
                    color = EmberOrange,
                    fontWeight = FontWeight.SemiBold,
                )
                Slider(
                    value = ttsSpeed,
                    onValueChange = { ttsSpeed = it },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    colors = configSliderColors(),
                )
                ConfigSwitch(stringResource(R.string.mcp_dynamic_thinking), stringResource(R.string.mcp_dynamic_thinking_desc), dynamicThinking) { dynamicThinking = it }
                ConfigSwitch(stringResource(R.string.mcp_emoji_tone), stringResource(R.string.mcp_emoji_tone_desc), emojiTone) { emojiTone = it }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_cloud_tts_keys))
                SecretField(stringResource(R.string.mcp_openai_tts_key), openaiKey) { openaiKey = it }
                SecretField(stringResource(R.string.mcp_elevenlabs_key), elevenlabsKey) { elevenlabsKey = it }
                SecretField(stringResource(R.string.mcp_deepgram_key), deepgramKey) { deepgramKey = it }
                Text(
                    stringResource(R.string.mcp_keys_stored_locally),
                    color = MutedGray,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SaveConfigButton {
            AppConfigManager.ttsEngine = ttsEngine
            AppConfigManager.ttsVoice = ttsVoice.trim()
            AppConfigManager.ttsSpeed = ttsSpeed
            AppConfigManager.openaiTtsApiKey = openaiKey.trim()
            AppConfigManager.elevenlabsApiKey = elevenlabsKey.trim()
            AppConfigManager.deepgramApiKey = deepgramKey.trim()
            AppConfigManager.dynamicThinkingEnabled = dynamicThinking
            AppConfigManager.emojiToneEnabled = emojiTone
        }
    }
}

@Composable
fun McpConfigScreen(onBack: () -> Unit) {
    var enabled by remember { mutableStateOf(AppConfigManager.mcpEnabled) }
    var sandboxOnly by remember { mutableStateOf(AppConfigManager.mcpSandboxOnly) }
    var serverList by remember { mutableStateOf(AppConfigManager.mcpServerList) }

    val parsedServers = remember(serverList) {
        try {
            val arr = org.json.JSONArray(serverList)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                MCPServerInfo(
                    name = obj.optString("name", ""),
                    command = obj.optString("command", ""),
                    args = obj.optString("args", ""),
                    status = try { ServerStatus.valueOf(obj.optString("status", "Stopped")) } catch (_: Exception) { ServerStatus.Stopped },
                    errorMessage = obj.optString("error", ""),
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun saveServerList(servers: List<MCPServerInfo>) {
        val arr = org.json.JSONArray(servers.map { s ->
            org.json.JSONObject().apply {
                put("name", s.name)
                put("command", s.command)
                put("args", s.args)
                put("status", s.status.name)
                put("error", s.errorMessage)
            }
        })
        serverList = arr.toString()
        AppConfigManager.mcpServerList = serverList
    }

    fun toggleConnector(key: String, @StringRes titleRes: Int, command: String, args: String = "") {
        val current = parsedServers.toMutableList()
        val existing = current.find { it.name == key }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(MCPServerInfo(name = key, command = command, args = args))
        }
        saveServerList(current)
    }

    ConfigScaffold(stringResource(R.string.mcp_connectors_title), Icons.Outlined.Link, onBack) {
        InfoCard(
            title = stringResource(R.string.mcp_mcp_connectors),
            body = stringResource(R.string.mcp_connectors_body)
        )

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.mcp_mcp_runtime), color = SoftWhite, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.mcp_master_switch_desc), color = MutedGray, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.4f),
                            uncheckedThumbColor = MutedGray,
                            uncheckedTrackColor = DeepBlack,
                        ),
                    )
                }
                AnimatedVisibility(visible = enabled) {
                    ConfigSwitch(stringResource(R.string.mcp_sandbox_only), stringResource(R.string.mcp_sandbox_only_desc), sandboxOnly) { sandboxOnly = it }
                }
            }
        }

        val connectors = listOf(
            ConnectorDef("filesystem", R.string.mcp_connector_filesystem, Icons.Outlined.Folder, "npx @modelcontextprotocol/server-filesystem", "/home", R.string.mcp_connector_filesystem_desc),
            ConnectorDef("github", R.string.mcp_connector_github, Icons.Outlined.Code, "python -m mcp_github", "", R.string.mcp_connector_github_desc),
            ConnectorDef("browser", R.string.mcp_connector_browser, Icons.Outlined.Language, "npx @anthropic/mcp-browser", "", R.string.mcp_connector_browser_desc),
            ConnectorDef("sqlite", R.string.mcp_connector_sqlite, Icons.Outlined.Storage, "npx @anthropic/mcp-database-server sqlite", "", R.string.mcp_connector_sqlite_desc),
            ConnectorDef("calendar", R.string.mcp_connector_calendar, Icons.Outlined.CalendarMonth, "npx @anthropic/mcp-google-calendar", "", R.string.mcp_connector_calendar_desc),
            ConnectorDef("email", R.string.mcp_connector_email, Icons.Outlined.MailOutline, "npx @anthropic/mcp-email", "", R.string.mcp_connector_email_desc),
            ConnectorDef("web-search", R.string.mcp_connector_web_search, Icons.Outlined.Search, "python -m mcp_web_search", "", R.string.mcp_connector_web_search_desc),
        )

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_available_connectors))

                connectors.forEach { def ->
                    val isConnected = parsedServers.any { it.name == def.key }
                    val connector = parsedServers.find { it.name == def.key }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isConnected) GlassFillStrong else GlassFill)
                            .border(1.dp, if (isConnected) NeonCyan.copy(alpha = 0.3f) else GlassBorderDim, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(def.icon, contentDescription = null, tint = if (isConnected) NeonCyan else MutedGray, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(def.titleRes), color = SoftWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(stringResource(def.descriptionRes), color = MutedGray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isConnected,
                                onCheckedChange = { toggleConnector(def.key, def.titleRes, def.command, def.args) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonCyan,
                                    checkedTrackColor = NeonCyan.copy(alpha = 0.4f),
                                    uncheckedThumbColor = MutedGray,
                                    uncheckedTrackColor = DeepBlack,
                                ),
                            )
                        }

                        if (isConnected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.mcp_active), color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("\"${def.command} ${def.args}\"", color = MutedGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        SaveConfigButton {
            AppConfigManager.mcpEnabled = enabled
            AppConfigManager.mcpSandboxOnly = sandboxOnly
            AppConfigManager.mcpServerList = serverList
        }
    }
}

private data class ConnectorDef(
    val key: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val command: String,
    val args: String,
    @StringRes val descriptionRes: Int,
)

@Composable
fun SkillsConfigScreen(onBack: () -> Unit) {
    var storeEnabled by remember { mutableStateOf(AppConfigManager.skillStoreEnabled) }
    var skillUrls by remember { mutableStateOf(AppConfigManager.mcpServers) }

    ConfigScaffold(stringResource(R.string.mcp_skills_title), Icons.Outlined.Extension, onBack) {
        InfoCard(
            title = stringResource(R.string.mcp_skill_system),
            body = stringResource(R.string.mcp_skill_system_body)
        )

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_skill_sources))
                ConfigSwitch(stringResource(R.string.mcp_enable_skills_store), stringResource(R.string.mcp_enable_skills_store_desc), storeEnabled) { storeEnabled = it }

                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(stringResource(R.string.mcp_community_registry), "https://skills.sh")
                DetailRow(stringResource(R.string.mcp_local_prompts), "~/skills/*.md")
                DetailRow(stringResource(R.string.mcp_script_skills), "~/skills/*.sh")
                DetailRow(stringResource(R.string.mcp_install_command), "skills.sh install <skill-name>")
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.mcp_available_skills))
                SkillCard(stringResource(R.string.mcp_skill_web_researcher), stringResource(R.string.mcp_skill_web_researcher_desc), Icons.Outlined.Cloud)
                SkillCard(stringResource(R.string.mcp_skill_code_reviewer), stringResource(R.string.mcp_skill_code_reviewer_desc), Icons.Outlined.Cloud)
                SkillCard("ClaudeDroid WhatsApp", stringResource(R.string.mcp_skill_whatsapp_desc), Icons.Outlined.Cloud)
                SkillCard(stringResource(R.string.mcp_skill_workflow_builder), stringResource(R.string.mcp_skill_workflow_builder_desc), Icons.Outlined.Cloud)
                SkillCard(stringResource(R.string.mcp_skill_finance_tracker), stringResource(R.string.mcp_skill_finance_tracker_desc), Icons.Outlined.Cloud)
                SkillCard(stringResource(R.string.mcp_skill_study_buddy), stringResource(R.string.mcp_skill_study_buddy_desc), Icons.Outlined.Cloud)
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.mcp_custom_skill_urls))
                GlassTextField(
                    value = skillUrls,
                    onValueChange = { skillUrls = it },
                    placeholder = "https://skills.sh/my-custom-skill\nhttps://github.com/user/skill-repo",
                    singleLine = false,
                    maxLines = 4,
                )
            }
        }

        SaveConfigButton {
            AppConfigManager.skillStoreEnabled = storeEnabled
            AppConfigManager.mcpServers = skillUrls.trim()
        }
    }
}

@Composable
fun AutomationsConfigScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var heartbeatEnabled by remember { mutableStateOf(AppConfigManager.heartbeatEnabled) }
    var interval by remember { mutableStateOf(AppConfigManager.heartbeatIntervalMin) }
    var ultraAgent by remember { mutableStateOf(AppConfigManager.ultraAgentEnabled) }
    var approvalMode by remember { mutableStateOf(AppConfigManager.approvalMode) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {}

    ConfigScaffold(stringResource(R.string.mcp_automations_title), Icons.Outlined.CalendarMonth, onBack) {
        InfoCard(
            title = stringResource(R.string.mcp_background_automation),
            body = stringResource(R.string.mcp_background_automation_body),
        )

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_heartbeat_scanner))
                ConfigSwitch(stringResource(R.string.mcp_autonomous_heartbeat), stringResource(R.string.mcp_autonomous_heartbeat_desc), heartbeatEnabled) { heartbeatEnabled = it }

                AnimatedVisibility(visible = heartbeatEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(stringResource(R.string.mcp_cron_presets))
                        ConfigChoice(stringResource(R.string.mcp_every_15_min), stringResource(R.string.mcp_every_15_min_desc), interval == 15) { interval = 15 }
                        ConfigChoice(stringResource(R.string.mcp_every_30_min), stringResource(R.string.mcp_every_30_min_desc), interval == 30) { interval = 30 }
                        ConfigChoice(stringResource(R.string.mcp_hourly), stringResource(R.string.mcp_hourly_desc), interval == 60) { interval = 60 }
                        ConfigChoice(stringResource(R.string.mcp_every_2_hours), stringResource(R.string.mcp_every_2_hours_desc), interval == 120) { interval = 120 }

                        Text(
                            stringResource(R.string.mcp_scan_interval, interval),
                            color = EmberOrange,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Slider(
                            value = interval.toFloat(),
                            onValueChange = { interval = it.toInt() },
                            valueRange = 15f..120f,
                            steps = 7,
                            colors = configSliderColors(),
                        )
                        DetailRow(stringResource(R.string.mcp_task_file), stringResource(R.string.mcp_task_file_value))
                        DetailRow(stringResource(R.string.mcp_scheduler), stringResource(R.string.mcp_scheduler_value))
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_approval_mode))
                Text(
                    stringResource(R.string.mcp_approval_mode_desc),
                    color = MutedGray,
                    style = MaterialTheme.typography.bodySmall,
                )
                ConfigChoice(stringResource(R.string.mcp_approval_default), stringResource(R.string.mcp_approval_default_desc_auto), approvalMode == "default") { approvalMode = "default" }
                ConfigChoice(stringResource(R.string.mcp_approval_trusted), stringResource(R.string.mcp_approval_trusted_desc_auto), approvalMode == "trusted") { approvalMode = "trusted" }
                ConfigChoice(stringResource(R.string.mcp_approval_cautious), stringResource(R.string.mcp_approval_cautious_desc_auto), approvalMode == "cautious") { approvalMode = "cautious" }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigSwitch(stringResource(R.string.mcp_ultra_agent_mode), stringResource(R.string.mcp_ultra_agent_mode_desc), ultraAgent) {
                    ultraAgent = it
                    if (it) {
                        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                            && !Settings.canDrawOverlays(context)
                        ) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.mcp_cautious_hint),
                    color = MutedGray,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SaveConfigButton {
            AppConfigManager.heartbeatEnabled = heartbeatEnabled
            AppConfigManager.heartbeatIntervalMin = interval
            AppConfigManager.ultraAgentEnabled = ultraAgent
            AppConfigManager.approvalMode = approvalMode
            if (heartbeatEnabled) {
                com.clawdroid.app.core.automation.AutomationScheduler.schedule(context)
            }
            if (ultraAgent) {
                com.clawdroid.app.core.service.ServiceManager.start(context)
            } else {
                com.clawdroid.app.core.service.ServiceManager.stop(context)
            }
        }
    }
}

@Composable
fun AgentConfigScreen(onBack: () -> Unit) {
    var agentName by remember { mutableStateOf(AppConfigManager.agentName) }
    var agentPersonality by remember { mutableStateOf(AppConfigManager.agentPersonality) }
    var agentPurpose by remember { mutableStateOf(AppConfigManager.agentPurpose) }
    var ownerName by remember { mutableStateOf(AppConfigManager.ownerName) }
    var ownerInfo by remember { mutableStateOf(AppConfigManager.ownerInfo) }
    var maxTurns by remember { mutableStateOf(AppConfigManager.maxAgentTurns) }
    var approvalMode by remember { mutableStateOf(AppConfigManager.approvalMode) }
    var dynamicThinking by remember { mutableStateOf(AppConfigManager.dynamicThinkingEnabled) }
    var emojiTone by remember { mutableStateOf(AppConfigManager.emojiToneEnabled) }
    
    // Prompt Files
    var agentsMd by remember { mutableStateOf(AppConfigManager.agentsMd) }
    var soulMd by remember { mutableStateOf(AppConfigManager.soulMd) }
    var toolsMd by remember { mutableStateOf(AppConfigManager.toolsMd) }
    var skillMd by remember { mutableStateOf(AppConfigManager.skillMd) }
    var claudeMd by remember { mutableStateOf(AppConfigManager.claudeMd) }

    ConfigScaffold(stringResource(R.string.mcp_agent_config_title), Icons.Outlined.Security, onBack) {
        InfoCard(
            title = stringResource(R.string.mcp_agent_calibration),
            body = stringResource(R.string.mcp_agent_calibration_body),
        )

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_identity))
                GlassTextField(
                    value = agentName,
                    onValueChange = { agentName = it },
                    placeholder = stringResource(R.string.mcp_agent_name_placeholder),
                )
                GlassTextField(
                    value = agentPersonality,
                    onValueChange = { agentPersonality = it },
                    placeholder = stringResource(R.string.mcp_personality_placeholder),
                )
                GlassTextField(
                    value = agentPurpose,
                    onValueChange = { agentPurpose = it },
                    placeholder = stringResource(R.string.mcp_purpose_placeholder),
                )
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_prompt_instructions))
                
                GlassTextField(
                    value = agentsMd,
                    onValueChange = { agentsMd = it },
                    placeholder = stringResource(R.string.mcp_agents_md_placeholder),
                    singleLine = false,
                    maxLines = 4,
                )
                
                GlassTextField(
                    value = soulMd,
                    onValueChange = { soulMd = it },
                    placeholder = stringResource(R.string.mcp_soul_md_placeholder),
                    singleLine = false,
                    maxLines = 4,
                )
                
                GlassTextField(
                    value = toolsMd,
                    onValueChange = { toolsMd = it },
                    placeholder = stringResource(R.string.mcp_tools_md_placeholder),
                    singleLine = false,
                    maxLines = 4,
                )
                
                GlassTextField(
                    value = skillMd,
                    onValueChange = { skillMd = it },
                    placeholder = stringResource(R.string.mcp_skill_md_placeholder),
                    singleLine = false,
                    maxLines = 4,
                )
                
                GlassTextField(
                    value = claudeMd,
                    onValueChange = { claudeMd = it },
                    placeholder = stringResource(R.string.mcp_claude_md_placeholder),
                    singleLine = false,
                    maxLines = 4,
                )
                
                Text(
                    stringResource(R.string.mcp_prompt_files_note),
                    color = MutedGray,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_owner_context))
                GlassTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    placeholder = stringResource(R.string.mcp_owner_name_placeholder),
                )
                GlassTextField(
                    value = ownerInfo,
                    onValueChange = { ownerInfo = it },
                    placeholder = stringResource(R.string.mcp_owner_info_placeholder),
                    singleLine = false,
                    maxLines = 4,
                )
                Text(
                    stringResource(R.string.mcp_owner_context_note),
                    color = MutedGray,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_behavior))

                ConfigSwitch(stringResource(R.string.mcp_dynamic_thinking_phrases), stringResource(R.string.mcp_dynamic_thinking_phrases_desc), dynamicThinking) { dynamicThinking = it }
                ConfigSwitch(stringResource(R.string.mcp_emoji_tone_conversion), stringResource(R.string.mcp_emoji_tone_conversion_desc), emojiTone) { emojiTone = it }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.mcp_max_agent_turns, maxTurns),
                    color = EmberOrange,
                    fontWeight = FontWeight.SemiBold,
                )
                Slider(
                    value = maxTurns.toFloat(),
                    onValueChange = { maxTurns = it.toInt() },
                    valueRange = 20f..300f,
                    steps = 13,
                    colors = configSliderColors(),
                )
                Text(
                    stringResource(R.string.mcp_max_turns_note),
                    color = MutedGray,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.mcp_approval_mode))
                ConfigChoice(stringResource(R.string.mcp_approval_default), stringResource(R.string.mcp_approval_default_desc), approvalMode == "default") { approvalMode = "default" }
                ConfigChoice(stringResource(R.string.mcp_approval_trusted), stringResource(R.string.mcp_approval_trusted_desc), approvalMode == "trusted") { approvalMode = "trusted" }
                ConfigChoice(stringResource(R.string.mcp_approval_cautious), stringResource(R.string.mcp_approval_cautious_desc), approvalMode == "cautious") { approvalMode = "cautious" }
            }
        }

        SaveConfigButton {
            AppConfigManager.agentName = agentName.trim().ifBlank { "Claude" }
            AppConfigManager.agentPersonality = agentPersonality.trim().ifBlank { "Professional" }
            AppConfigManager.agentPurpose = agentPurpose.trim().ifBlank { "General assistant" }
            AppConfigManager.ownerName = ownerName.trim()
            AppConfigManager.ownerInfo = ownerInfo.trim()
            AppConfigManager.maxAgentTurns = maxTurns
            AppConfigManager.approvalMode = approvalMode
            AppConfigManager.dynamicThinkingEnabled = dynamicThinking
            AppConfigManager.emojiToneEnabled = emojiTone
            
            AppConfigManager.agentsMd = agentsMd.trim()
            AppConfigManager.soulMd = soulMd.trim()
            AppConfigManager.toolsMd = toolsMd.trim()
            AppConfigManager.skillMd = skillMd.trim()
            AppConfigManager.claudeMd = claudeMd.trim()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScaffold(
    title: String,
    icon: ImageVector,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text(title, color = SoftWhite, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.mcp_back),
                            tint = SoftWhite,
                        )
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConfigScreenHeader(title, icon)
            content()
        }
    }
}

@Composable
private fun ConfigScreenHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(GlassFillStrong),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = SoftWhite, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.mcp_configure_clawdroid), color = MutedGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                color = EmberOrange,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(body, color = MutedGray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = EmberOrange,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun ConfigChoice(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) GlassFillStrong else GlassFill)
            .border(
                1.dp,
                if (selected) EmberOrange else GlassBorderDim,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = SoftWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                description,
                color = MutedGray,
                fontSize = 12.sp,
            )
        }
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = EmberOrange,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ConfigSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SoftWhite, fontWeight = FontWeight.Bold)
            Text(
                description,
                color = MutedGray,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmberOrange,
                checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                uncheckedThumbColor = MutedGray,
                uncheckedTrackColor = DeepBlack,
            ),
        )
    }
}

@Composable
private fun SecretField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = MutedGray, style = MaterialTheme.typography.bodySmall)
        GlassTextField(
            value = value,
            onValueChange = onChange,
            placeholder = label,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = MutedGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            value,
            color = SoftWhite,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun StatusLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = EmberOrange,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = EmberOrange, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SkillCard(
    title: String,
    description: String,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorderDim, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = SoftWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Text(
                description,
                color = MutedGray,
                fontSize = 11.sp,
            )
        }
        Text(
            stringResource(R.string.mcp_skill_install),
            color = EmberOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SaveConfigButton(onSave: () -> Unit) {
    val context = LocalContext.current
    GlassButton(
        onClick = {
            onSave()
            Toast.makeText(context, context.getString(R.string.mcp_settings_saved), Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Save,
                contentDescription = null,
                tint = SoftWhite,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.mcp_save_changes), color = SoftWhite, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun configSliderColors() = SliderDefaults.colors(
    thumbColor = EmberOrange,
    activeTrackColor = EmberOrange,
    inactiveTrackColor = GlassBorderDim,
)
