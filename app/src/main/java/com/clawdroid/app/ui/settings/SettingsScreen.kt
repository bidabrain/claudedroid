package com.clawdroid.app.ui.settings

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawdroid.app.R
import com.clawdroid.app.core.config.AppConfigManager
import com.clawdroid.app.core.config.AppLanguage
import com.clawdroid.app.core.control.AndroidControlTools
import com.clawdroid.app.core.control.ScreenCaptureManager
import com.clawdroid.app.core.control.ScreenReaderService
import com.clawdroid.app.core.service.ServiceManager
import com.clawdroid.app.core.voice.PiperEngine
import com.clawdroid.app.ui.components.GlassButton
import com.clawdroid.app.ui.components.GlassCard
import com.clawdroid.app.ui.components.GlassTextField
import com.clawdroid.app.ui.components.GlowText
import com.clawdroid.app.ui.components.PiperDownloadDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import com.clawdroid.app.ui.theme.DeepBlack
import com.clawdroid.app.ui.theme.EmberOrange
import com.clawdroid.app.ui.theme.GlassBorderDim
import com.clawdroid.app.ui.theme.GlassFill
import com.clawdroid.app.ui.theme.GlassFillStrong
import com.clawdroid.app.ui.theme.MutedGray
import com.clawdroid.app.ui.theme.SoftWhite

// ── TTS engine options ──────────────────────────────────────────────────

private data class TtsEngineOption(
    val id: String,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val icon: ImageVector,
)

private val ttsEngineOptions = listOf(
    TtsEngineOption("device", R.string.settings_tts_device, R.string.settings_tts_device_desc, Icons.Outlined.Android),
    TtsEngineOption("openai", R.string.settings_tts_openai, R.string.settings_tts_openai_desc, Icons.Outlined.Cloud),
    TtsEngineOption("elevenlabs", R.string.settings_tts_elevenlabs, R.string.settings_tts_elevenlabs_desc, Icons.Outlined.Cloud),
    TtsEngineOption("deepgram", R.string.settings_tts_deepgram, R.string.settings_tts_deepgram_desc, Icons.Outlined.Cloud),
)

private val openaiVoices = listOf(
    "alloy" to R.string.settings_voice_alloy,
    "echo" to R.string.settings_voice_echo,
    "fable" to R.string.settings_voice_fable,
    "onyx" to R.string.settings_voice_onyx,
    "nova" to R.string.settings_voice_nova,
    "shimmer" to R.string.settings_voice_shimmer,
)

private val realtimeVoices = listOf(
    "marin" to "Marin",
    "cedar" to "Cedar",
)

// ── Main Screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToWorkspaceFiles: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onBack()
    }
    var showKey by remember { mutableStateOf(false) }

    var ttsEngine by remember { mutableStateOf(AppConfigManager.ttsEngine) }
    var ttsVoice by remember { mutableStateOf(AppConfigManager.ttsVoice) }
    var ttsSpeed by remember { mutableStateOf(AppConfigManager.ttsSpeed) }
    var realtimeVoiceEnabled by remember { mutableStateOf(AppConfigManager.realtimeVoiceEnabled) }
    var realtimeVoiceModel by remember { mutableStateOf(AppConfigManager.realtimeVoiceModel) }
    var realtimeVoiceVoice by remember { mutableStateOf(AppConfigManager.realtimeVoiceVoice) }

    var openaiTtsApiKey by remember { mutableStateOf(AppConfigManager.openaiTtsApiKey) }
    var openaiRealtimeApiKey by remember { mutableStateOf(AppConfigManager.openaiRealtimeApiKey) }
    var elevenlabsApiKey by remember { mutableStateOf(AppConfigManager.elevenlabsApiKey) }
    var deepgramApiKey by remember { mutableStateOf(AppConfigManager.deepgramApiKey) }

    var saved by remember { mutableStateOf(false) }
    var storagePermitted by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Piper engine for optional download
    val piperEngine = remember { PiperEngine(context.applicationContext) }
    val piperDownloadProgress by piperEngine.downloadProgress.collectAsState()
    val piperInstalled = piperEngine.isInstalled
    val piperDownloading = piperDownloadProgress > 0f && piperDownloadProgress < 1f

    // Test voice TTS
    var testTts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { }
        testTts = tts
        onDispose { tts.shutdown() }
    }
    var isUltraAgentEnabled by remember { mutableStateOf(AppConfigManager.ultraAgentEnabled) }
    var showWarningDialog by remember { mutableStateOf(false) }

    var whatsappEnabled by remember { mutableStateOf(AppConfigManager.whatsappEnabled) }
    var whatsappAllowedContacts by remember { mutableStateOf(AppConfigManager.whatsappAllowedContacts) }
    var smsEnabled by remember { mutableStateOf(AppConfigManager.smsEnabled) }
    var heartbeatEnabled by remember { mutableStateOf(AppConfigManager.heartbeatEnabled) }
    var heartbeatIntervalMin by remember { mutableStateOf(AppConfigManager.heartbeatIntervalMin) }
    var notificationAccessGranted by remember { mutableStateOf(false) }

    var assistantModeEnabled by remember { mutableStateOf(AppConfigManager.assistantModeEnabled) }
    var doodleOverlayEnabled by remember { mutableStateOf(AppConfigManager.doodleOverlayEnabled) }
    var screenContextEnabled by remember { mutableStateOf(AppConfigManager.screenContextEnabled) }
    var saveScreenshotsToHistory by remember { mutableStateOf(AppConfigManager.saveScreenshotsToHistory) }
    var accessibilityActive by remember { mutableStateOf(ScreenReaderService.instance != null) }
    var screenCaptureActive by remember { mutableStateOf(ScreenCaptureManager.isActive()) }
    var showScreenTestDialog by remember { mutableStateOf(false) }
    var screenTestResult by remember { mutableStateOf("") }
    var screenTestLoading by remember { mutableStateOf(false) }

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val ok = ScreenCaptureManager.startCapture(context, result.resultCode, result.data!!)
            screenCaptureActive = ok
            Toast.makeText(
                context,
                if (ok) context.getString(R.string.settings_toast_capture_active) else context.getString(R.string.settings_toast_capture_failed),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val saveAndSync = {
        AppConfigManager.ttsEngine = ttsEngine
        AppConfigManager.ttsVoice = ttsVoice.trim()
        AppConfigManager.ttsSpeed = ttsSpeed
        AppConfigManager.realtimeVoiceEnabled = realtimeVoiceEnabled
        AppConfigManager.realtimeVoiceModel = realtimeVoiceModel.trim().ifBlank { "gpt-realtime-2" }
        AppConfigManager.realtimeVoiceVoice = realtimeVoiceVoice.trim().ifBlank { "marin" }
        AppConfigManager.openaiTtsApiKey = openaiTtsApiKey.trim()
        AppConfigManager.openaiRealtimeApiKey = openaiRealtimeApiKey.trim()
        AppConfigManager.elevenlabsApiKey = elevenlabsApiKey.trim()
        AppConfigManager.deepgramApiKey = deepgramApiKey.trim()
        AppConfigManager.ultraAgentEnabled = isUltraAgentEnabled
        AppConfigManager.whatsappEnabled = whatsappEnabled
        AppConfigManager.whatsappAllowedContacts = whatsappAllowedContacts.trim()
        AppConfigManager.smsEnabled = smsEnabled
        AppConfigManager.heartbeatEnabled = heartbeatEnabled
        AppConfigManager.heartbeatIntervalMin = heartbeatIntervalMin
        AppConfigManager.assistantModeEnabled = assistantModeEnabled
        AppConfigManager.doodleOverlayEnabled = doodleOverlayEnabled
        AppConfigManager.screenContextEnabled = screenContextEnabled
        AppConfigManager.saveScreenshotsToHistory = saveScreenshotsToHistory
        AppConfigManager.syncToSandbox(context)
    }

    // Check notification listener access on resume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val cn = ComponentName(context, com.clawdroid.app.core.channels.ClawNotificationListenerService::class.java)
                val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                notificationAccessGranted = flat != null && flat.contains(cn.flattenToString())
                accessibilityActive = ScreenReaderService.instance != null
                screenCaptureActive = ScreenCaptureManager.isActive()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Toast.makeText(context, context.getString(R.string.settings_toast_permissions_updated), Toast.LENGTH_SHORT).show()
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        storagePermitted = if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    val requestUltraAgentPermissions = {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                Toast.makeText(context, context.getString(R.string.settings_toast_enable_overlay), Toast.LENGTH_LONG).show()
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        }

        val accessibilityIntent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(accessibilityIntent)
    }

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        color = SoftWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = SoftWhite,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlack,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlack)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Language ─────────────────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_language),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val currentLanguage = remember { AppLanguage.get(context) }
                    listOf(
                        Triple(AppLanguage.ZH, "简体中文", R.string.settings_language_zh_desc),
                        Triple(AppLanguage.EN, "English", R.string.settings_language_en_desc),
                        Triple(AppLanguage.SYSTEM, stringResource(R.string.settings_language_system), R.string.settings_language_system_desc),
                    ).forEach { (code, label, desc) ->
                        SelectableCard(
                            label = label,
                            description = stringResource(desc),
                            isSelected = currentLanguage == code,
                            onClick = {
                                if (code != currentLanguage) {
                                    AppLanguage.set(context, code)
                                    // Android 13+ recreates the activity itself when the app locale changes.
                                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                                        (context as? Activity)?.recreate()
                                    }
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Voice & Speech ───────────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_voice),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_realtime_voice), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            Text(
                                text = stringResource(R.string.settings_realtime_voice_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        Switch(
                            checked = realtimeVoiceEnabled,
                            onCheckedChange = {
                                realtimeVoiceEnabled = it
                                saved = false
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SoftWhite,
                                checkedTrackColor = EmberOrange,
                            ),
                        )
                    }

                    AnimatedVisibility(visible = realtimeVoiceEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.settings_realtime_api_key), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            Text(
                                text = stringResource(R.string.settings_realtime_api_key_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                            GlassTextField(
                                value = openaiRealtimeApiKey,
                                onValueChange = { openaiRealtimeApiKey = it; saved = false },
                                placeholder = stringResource(R.string.settings_realtime_api_key_hint),
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )

                            Text(stringResource(R.string.settings_realtime_model), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            GlassTextField(
                                value = realtimeVoiceModel,
                                onValueChange = { realtimeVoiceModel = it; saved = false },
                                placeholder = "gpt-realtime-2",
                            )

                            Text(stringResource(R.string.settings_realtime_voice), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            realtimeVoices.forEach { (id, label) ->
                                val isSelected = realtimeVoiceVoice == id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GlassFillStrong else GlassFill)
                                        .border(1.dp, if (isSelected) EmberOrange else GlassBorderDim, RoundedCornerShape(12.dp))
                                        .clickable { realtimeVoiceVoice = id; saved = false }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = label,
                                        color = SoftWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp,
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = EmberOrange,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_tts_engine), style = MaterialTheme.typography.labelLarge, color = EmberOrange)

                    ttsEngineOptions.forEach { option ->
                        val isSelected = ttsEngine == option.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) GlassFillStrong else GlassFill)
                                .border(1.dp, if (isSelected) EmberOrange else GlassBorderDim, RoundedCornerShape(14.dp))
                                .clickable { ttsEngine = option.id; saved = false }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) EmberOrange else MutedGray,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(option.label),
                                    color = SoftWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = stringResource(option.description),
                                    color = MutedGray,
                                    fontSize = 12.sp,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = EmberOrange,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    // ── Engine-specific config ───────────────────────────
                    when (ttsEngine) {
                        "openai" -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.settings_openai_api_key), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            Text(
                                text = stringResource(R.string.settings_openai_tts_key_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                            GlassTextField(
                                value = openaiTtsApiKey,
                                onValueChange = { openaiTtsApiKey = it; saved = false },
                                placeholder = stringResource(R.string.settings_openai_tts_key_hint),
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.settings_openai_voice), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            openaiVoices.forEach { (id, label) ->
                                val isSelected = ttsVoice == id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GlassFillStrong else GlassFill)
                                        .border(1.dp, if (isSelected) EmberOrange else GlassBorderDim, RoundedCornerShape(12.dp))
                                        .clickable { ttsVoice = id; saved = false }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(label),
                                        color = SoftWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp,
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = EmberOrange,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }

                        "elevenlabs" -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.settings_elevenlabs_api_key), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            GlassTextField(
                                value = elevenlabsApiKey,
                                onValueChange = { elevenlabsApiKey = it; saved = false },
                                placeholder = stringResource(R.string.settings_elevenlabs_api_key_hint),
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.settings_elevenlabs_voice), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            com.clawdroid.app.core.voice.ElevenLabsTtsEngine.PRESET_VOICES.forEach { (id, label) ->
                                val isSelected = ttsVoice == id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GlassFillStrong else GlassFill)
                                        .border(1.dp, if (isSelected) EmberOrange else GlassBorderDim, RoundedCornerShape(12.dp))
                                        .clickable { ttsVoice = id; saved = false }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = label,
                                        color = SoftWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp,
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = EmberOrange,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }

                        "deepgram" -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.settings_deepgram_api_key), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            GlassTextField(
                                value = deepgramApiKey,
                                onValueChange = { deepgramApiKey = it; saved = false },
                                placeholder = stringResource(R.string.settings_deepgram_api_key_hint),
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.settings_deepgram_voice), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            com.clawdroid.app.core.voice.DeepgramTtsEngine.PRESET_VOICES.forEach { (id, label) ->
                                val isSelected = ttsVoice == id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GlassFillStrong else GlassFill)
                                        .border(1.dp, if (isSelected) EmberOrange else GlassBorderDim, RoundedCornerShape(12.dp))
                                        .clickable { ttsVoice = id; saved = false }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = label,
                                        color = SoftWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp,
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = EmberOrange,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_speech_speed, String.format("%.1f", ttsSpeed)),
                        style = MaterialTheme.typography.labelLarge,
                        color = EmberOrange,
                    )
                    Slider(
                        value = ttsSpeed,
                        onValueChange = { ttsSpeed = it; saved = false },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = EmberOrange,
                            activeTrackColor = EmberOrange,
                            inactiveTrackColor = GlassBorderDim,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Piper download card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (piperInstalled) GlassFillStrong.copy(alpha = 0.5f) else GlassFill)
                            .border(1.dp, if (piperInstalled) EmberOrange.copy(alpha = 0.4f) else GlassBorderDim, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (piperInstalled) Icons.Rounded.CheckCircle else Icons.Outlined.Headphones,
                                contentDescription = null,
                                tint = if (piperInstalled) EmberOrange else MutedGray,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (piperInstalled) stringResource(R.string.settings_piper_installed) else stringResource(R.string.settings_piper_title),
                                    color = SoftWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = if (piperInstalled) stringResource(R.string.settings_piper_installed_desc) else stringResource(R.string.settings_piper_download_desc),
                                    color = MutedGray,
                                    fontSize = 12.sp,
                                )
                            }
                            if (!piperInstalled) {
                                GlassButton(
                                    onClick = { piperEngine.startDownload() },
                                    modifier = Modifier.width(100.dp).height(36.dp),
                                ) {
                                    Text(stringResource(R.string.settings_download), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SoftWhite)
                                }
                            }
                        }
                    }

                    // Test Voice button
                    GlassButton(
                        onClick = {
                            testTts?.let { tts ->
                                tts.language = AppLanguage.current(context)
                                tts.setPitch(0.75f)
                                tts.setSpeechRate(0.82f * ttsSpeed)
                                tts.speak(
                                    context.getString(R.string.settings_test_voice_phrase, AppConfigManager.agentName),
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "test"
                                )
                            }
                        },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Headphones,
                                contentDescription = null,
                                tint = SoftWhite,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_test_voice), fontWeight = FontWeight.SemiBold, color = SoftWhite)
                        }
                    }
                }
            }

            // Piper download progress dialog
            if (piperDownloading) {
                PiperDownloadDialog(progress = piperDownloadProgress)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Agent ────────────────────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_agent),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_approval_mode), style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                        }
                        Text(stringResource(R.string.settings_approval_mode_default), style = MaterialTheme.typography.bodyMedium, color = SoftWhite, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Android,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_sandbox), style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                        }
                        Text(stringResource(R.string.settings_full_auto), style = MaterialTheme.typography.bodyMedium, color = SoftWhite, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_ultra_agent_mode),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isUltraAgentEnabled) Color(0xFFEF5350) else SoftWhite,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.settings_ultra_agent_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        Switch(
                            checked = isUltraAgentEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showWarningDialog = true
                                } else {
                                    isUltraAgentEnabled = false
                                    AppConfigManager.ultraAgentEnabled = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFEF5350),
                                checkedTrackColor = Color(0xFFEF5350).copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DeepBlack,
                            ),
                        )
                    }

                    Text(
                        text = stringResource(R.string.settings_autonomy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray.copy(alpha = 0.7f),
                    )
                }
            }

            if (showWarningDialog) {
                AlertDialog(
                    onDismissRequest = { showWarningDialog = false },
                    title = {
                        Text(
                            text = stringResource(R.string.settings_ultra_warning_title),
                            color = Color(0xFFEF5350),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.settings_ultra_warning_text),
                            color = SoftWhite,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    confirmButton = {
                        GlassButton(
                            onClick = {
                                showWarningDialog = false
                                isUltraAgentEnabled = true
                                AppConfigManager.ultraAgentEnabled = true
                                requestUltraAgentPermissions()
                            },
                        ) {
                            Text(stringResource(R.string.settings_ultra_confirm), color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWarningDialog = false }) {
                            Text(stringResource(R.string.settings_cancel), color = SoftWhite)
                        }
                    },
                    containerColor = DeepBlack,
                    tonalElevation = 6.dp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Android Control ─────────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_android_control),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_accessibility_service),
                                style = MaterialTheme.typography.bodyLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (accessibilityActive) stringResource(R.string.settings_screen_control_active) else stringResource(R.string.settings_accessibility_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (accessibilityActive) Color(0xFF66BB6A) else MutedGray,
                            )
                        }
                        if (accessibilityActive) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF66BB6A),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    GlassButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (accessibilityActive) stringResource(R.string.settings_manage_accessibility) else stringResource(R.string.settings_enable_accessibility),
                            color = SoftWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_screen_capture),
                                style = MaterialTheme.typography.bodyLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (screenCaptureActive) stringResource(R.string.settings_vision_fallback_active) else stringResource(R.string.settings_vision_fallback_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (screenCaptureActive) Color(0xFF66BB6A) else MutedGray,
                            )
                        }
                        if (screenCaptureActive) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF66BB6A),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GlassButton(
                            onClick = {
                                screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.settings_grant_capture), color = SoftWhite, fontWeight = FontWeight.SemiBold)
                        }
                        if (screenCaptureActive) {
                            GlassButton(
                                onClick = {
                                    ScreenCaptureManager.stopCapture()
                                    screenCaptureActive = false
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.settings_stop_capture), color = SoftWhite, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    GlassButton(
                        onClick = {
                            screenTestLoading = true
                            scope.launch {
                                val result = AndroidControlTools.getScreen(context).toString(2)
                                screenTestResult = if (result.length > 8000) {
                                    result.take(8000) + context.getString(R.string.settings_truncated_suffix)
                                } else {
                                    result
                                }
                                screenTestLoading = false
                                showScreenTestDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !screenTestLoading,
                    ) {
                        Text(
                            if (screenTestLoading) stringResource(R.string.settings_reading_screen) else stringResource(R.string.settings_test_screen_read),
                            color = SoftWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (showScreenTestDialog) {
                AlertDialog(
                    onDismissRequest = { showScreenTestDialog = false },
                    title = {
                        Text(stringResource(R.string.settings_screen_read_result), color = SoftWhite, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            val noResultText = stringResource(R.string.settings_no_result)
                            Text(
                                text = screenTestResult.ifBlank { noResultText },
                                color = SoftWhite,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showScreenTestDialog = false }) {
                            Text(stringResource(R.string.settings_close), color = SoftWhite)
                        }
                    },
                    containerColor = DeepBlack,
                    tonalElevation = 6.dp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── System Assistant ─────────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_system_assistant),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isDefault = remember { mutableStateOf(com.clawdroid.app.core.assistant.permissions.AssistantPermissionCoordinator.isDefaultAssistant(context)) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_default_assistant),
                                style = MaterialTheme.typography.bodyLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (isDefault.value) stringResource(R.string.settings_is_default_assistant) else stringResource(R.string.settings_select_default_assistant),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDefault.value) Color(0xFF66BB6A) else MutedGray,
                            )
                        }
                        if (isDefault.value) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF66BB6A),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    GlassButton(
                        onClick = {
                            val intent = com.clawdroid.app.core.assistant.permissions.AssistantPermissionCoordinator.getRecoveryIntent(
                                context,
                                com.clawdroid.app.core.assistant.permissions.PermissionRecoveryAction.REQUEST_ROLE
                            )
                            if (intent != null) {
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (isDefault.value) stringResource(R.string.settings_manage_assistant) else stringResource(R.string.settings_set_default_assistant),
                            color = SoftWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_assistant_mode), color = SoftWhite, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.settings_assistant_mode_desc), style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        }
                        Switch(
                            checked = assistantModeEnabled,
                            onCheckedChange = { assistantModeEnabled = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmberOrange,
                                checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DeepBlack,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_doodle_overlay), color = SoftWhite, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.settings_doodle_overlay_desc), style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        }
                        Switch(
                            checked = doodleOverlayEnabled,
                            onCheckedChange = { doodleOverlayEnabled = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmberOrange,
                                checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DeepBlack,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_screen_context), color = SoftWhite, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.settings_screen_context_desc), style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        }
                        Switch(
                            checked = screenContextEnabled,
                            onCheckedChange = { screenContextEnabled = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmberOrange,
                                checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DeepBlack,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_save_screenshots), color = SoftWhite, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.settings_save_screenshots_desc), style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        }
                        Switch(
                            checked = saveScreenshotsToHistory,
                            onCheckedChange = { saveScreenshotsToHistory = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmberOrange,
                                checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DeepBlack,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Skills & Channels ───────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_skills_channels),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.settings_workspace_files),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.settings_workspace_files_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    GlassButton(onClick = onNavigateToWorkspaceFiles, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_open_workspace_files), color = SoftWhite, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_whatsapp_automation),
                                style = MaterialTheme.typography.bodyLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.settings_whatsapp_automation_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        Switch(
                            checked = whatsappEnabled,
                            onCheckedChange = { whatsappEnabled = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmberOrange,
                                checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DeepBlack,
                            ),
                        )
                    }

                    AnimatedVisibility(visible = whatsappEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!notificationAccessGranted) {
                                GlassButton(
                                    onClick = {
                                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.settings_grant_notification_access), color = EmberOrange, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = EmberOrange,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.settings_notification_access_granted), color = EmberOrange, fontWeight = FontWeight.Medium)
                                }
                            }

                            Text(stringResource(R.string.settings_allowed_contacts), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            GlassTextField(
                                value = whatsappAllowedContacts,
                                onValueChange = { whatsappAllowedContacts = it; saved = false },
                                placeholder = stringResource(R.string.settings_allowed_contacts_hint_all),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorderDim))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Heartbeat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_heartbeat),
                                style = MaterialTheme.typography.bodyLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.settings_heartbeat_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        Switch(
                            checked = heartbeatEnabled,
                            onCheckedChange = { heartbeatEnabled = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmberOrange,
                                checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DeepBlack,
                            ),
                        )
                    }

                    AnimatedVisibility(visible = heartbeatEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.settings_heartbeat_interval, heartbeatIntervalMin),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = heartbeatIntervalMin.toFloat(),
                                onValueChange = { heartbeatIntervalMin = it.toInt(); saved = false },
                                valueRange = 15f..120f,
                                steps = 7, // 15, 30, 45, 60, 75, 90, 105, 120
                                colors = SliderDefaults.colors(
                                    thumbColor = EmberOrange,
                                    activeTrackColor = EmberOrange,
                                    inactiveTrackColor = GlassBorderDim
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Background Agent ─────────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_background_agent),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_background_mode), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                            Text(
                                text = stringResource(R.string.settings_background_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        Switch(
                            checked = isUltraAgentEnabled,
                            onCheckedChange = { isUltraAgentEnabled = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmberOrange,
                                checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DeepBlack,
                            ),
                        )
                    }

                    if (isUltraAgentEnabled) {
                        // Service status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("●", color = EmberOrange, fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_service_active),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }

                        // WhatsApp channel toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_whatsapp_channel), color = SoftWhite, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(R.string.settings_whatsapp_channel_desc), style = MaterialTheme.typography.bodySmall, color = MutedGray)
                            }
                            Switch(
                                checked = whatsappEnabled,
                                onCheckedChange = { whatsappEnabled = it; saved = false },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmberOrange,
                                    checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                    uncheckedThumbColor = MutedGray,
                                    uncheckedTrackColor = DeepBlack,
                                ),
                            )
                        }

                        if (whatsappEnabled) {
                            Text(stringResource(R.string.settings_allowed_contacts), style = MaterialTheme.typography.bodySmall, color = MutedGray)
                            GlassTextField(
                                value = whatsappAllowedContacts,
                                onValueChange = { whatsappAllowedContacts = it; saved = false },
                                placeholder = stringResource(R.string.settings_allowed_contacts_hint),
                            )
                        }

                        // SMS channel toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_sms_channel), color = SoftWhite, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(R.string.settings_sms_channel_desc), style = MaterialTheme.typography.bodySmall, color = MutedGray)
                            }
                            Switch(
                                checked = smsEnabled,
                                onCheckedChange = { smsEnabled = it; saved = false },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmberOrange,
                                    checkedTrackColor = EmberOrange.copy(alpha = 0.5f),
                                    uncheckedThumbColor = MutedGray,
                                    uncheckedTrackColor = DeepBlack,
                                ),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorderDim))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Agent config management
                        Text(stringResource(R.string.settings_agent_configuration), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                        Text(
                            text = stringResource(R.string.settings_agent_configuration_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                        GlassButton(onClick = {
                            saveAndSync()
                            Toast.makeText(context, context.getString(R.string.settings_toast_config_saved), Toast.LENGTH_SHORT).show()
                        }) {
                            Text(stringResource(R.string.settings_export_config), fontWeight = FontWeight.SemiBold, color = SoftWhite)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── File Storage ─────────────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_file_storage),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_shared_folder), style = MaterialTheme.typography.labelLarge, color = EmberOrange)
                    Text(
                        text = "Documents/ClaudeDroid/Inbox, Output, Projects, Exports",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )

                    if (!storagePermitted) {
                        GlassButton(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= 30) {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    storagePermissionLauncher.launch(intent)
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    )
                                }
                            },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Cloud,
                                    contentDescription = null,
                                    tint = SoftWhite,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_enable_file_access), fontWeight = FontWeight.SemiBold, color = SoftWhite)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmberOrange,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_file_access_granted), color = EmberOrange, fontWeight = FontWeight.Medium)
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_shared_folder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── About ────────────────────────────────────────
            GlowText(
                text = stringResource(R.string.settings_section_about),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                        }
                        Text("0.1.0", style = MaterialTheme.typography.bodyMedium, color = SoftWhite, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_architecture), style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                        }
                        Text("Kotlin + Compose", style = MaterialTheme.typography.bodyMedium, color = SoftWhite, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text = stringResource(R.string.settings_built_with),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Save button ──────────────────────────────────
            SaveButton(
                saved = saved,
                enabled = true,
                onClick = {
                    saveAndSync()
                    if (heartbeatEnabled) {
                        com.clawdroid.app.core.automation.AutomationScheduler.schedule(context)
                    }
                    // Start/stop background service based on toggle
                    if (isUltraAgentEnabled) {
                        com.clawdroid.app.core.service.ServiceManager.start(context)
                    } else {
                        com.clawdroid.app.core.service.ServiceManager.stop(context)
                    }
                    saved = true
                },
            )


            Spacer(modifier = Modifier.height(40.dp))
        }
    }

}

// ── Selectable Card (reusable for models, etc.) ─────────────────────────

@Composable
private fun SelectableCard(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) GlassFillStrong else GlassFill)
            .border(1.dp, if (isSelected) EmberOrange else GlassBorderDim, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = SoftWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                text = description,
                color = MutedGray,
                fontSize = 12.sp,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = EmberOrange,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Animated Save Button ────────────────────────────────────────────────

@Composable
private fun SaveButton(
    saved: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val saveAlpha by animateFloatAsState(
        targetValue = if (saved) 0.6f else 1f,
        animationSpec = tween(300),
        label = "save_alpha",
    )

    GlassButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = saved,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = SoftWhite,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (!saved) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = null,
                    tint = SoftWhite,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (saved) stringResource(R.string.settings_saved) else stringResource(R.string.settings_save_changes),
                fontWeight = FontWeight.SemiBold,
                color = SoftWhite,
                modifier = Modifier.alpha(saveAlpha),
            )
        }
    }
}
