package com.clawdroid.app.data.api

import android.util.Base64
import android.util.Log
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.helpers.MessageAccumulator
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ThinkingConfigAdaptive
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUnion
import com.anthropic.models.messages.ToolUseBlockParam
import com.clawdroid.app.core.config.AppConfigManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Duration

data class ChatMessage(
    val role: String,
    val content: String? = null,
    val toolCallId: String? = null,
    val toolCalls: List<CompletedToolCall> = emptyList(),
    val mediaPath: String? = null,
    val mediaMimeType: String? = null,
)

data class CompletedToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

/** [promptTokens] is the full prompt size (uncached + cache reads + cache writes); [cachedTokens] is cache reads. */
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val cachedTokens: Int = 0,
)

sealed interface StreamEvent {
    data class TextDelta(val text: String) : StreamEvent
    data class ToolCallDeltaReceived(val index: Int, val id: String, val name: String, val arguments: String) : StreamEvent
    data class ToolCallComplete(val call: CompletedToolCall) : StreamEvent
    data class Usage(val usage: TokenUsage) : StreamEvent
    data class Error(val message: String) : StreamEvent
    data object Done : StreamEvent
}

/**
 * Streams chat turns from Claude through the native Messages API.
 *
 * Callers speak the app's own [ChatMessage] / OpenAI-style tool JSON; this class converts to
 * Messages API params, marks the stable prefix (tools + system) and the growing conversation for
 * prompt caching, and replays each assistant turn's thinking blocks unchanged on later requests.
 */
class LlmApiClient(
    private val baseUrl: String = AppConfigManager.baseUrl,
    private val apiKey: String = AppConfigManager.apiKey,
    private val model: String = AppConfigManager.model,
) {
    @Suppress("UNUSED_PARAMETER")
    fun streamChat(
        messages: List<ChatMessage>,
        tools: JSONArray? = null,
        // Forced tool choice is rejected by current Claude models; kept for call-site compatibility.
        forcedToolName: String? = null,
    ): Flow<StreamEvent> = flow {
        check(apiKey.isNotBlank()) { "Missing LLM API key" }
        check(model.isNotBlank()) { "Missing LLM model" }

        val params = buildParams(messages, tools)
        Log.i(TAG, "streamChat model=$model messages=${messages.size} tools=${tools?.length() ?: 0}")

        val accumulator = MessageAccumulator.create()
        val toolArgs = mutableMapOf<Long, ToolStream>()
        val response = try {
            client(baseUrl, apiKey).messages().createStreaming(params)
        } catch (e: AnthropicServiceException) {
            emit(StreamEvent.Error("HTTP ${e.statusCode()}: ${e.message}"))
            return@flow
        }
        // Closing the response aborts the blocking read, so Stop takes effect immediately.
        val cancelHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { runCatching { response.close() } }
        try {
            response.use {
                val events = it.stream().iterator()
                while (events.hasNext()) {
                    val event = events.next()
                    accumulator.accumulate(event)

                    event.contentBlockStart().orElse(null)?.let { start ->
                        start.contentBlock().toolUse().orElse(null)?.let { use ->
                            toolArgs[start.index()] = ToolStream(use.id(), use.name())
                        }
                    }
                    val deltaEvent = event.contentBlockDelta().orElse(null) ?: continue
                    val delta = deltaEvent.delta()
                    delta.text().orElse(null)?.let { emit(StreamEvent.TextDelta(it.text())) }
                    delta.inputJson().orElse(null)?.let { json ->
                        val tool = toolArgs[deltaEvent.index()] ?: return@let
                        tool.arguments.append(json.partialJson())
                        emit(
                            StreamEvent.ToolCallDeltaReceived(
                                index = deltaEvent.index().toInt(),
                                id = tool.id,
                                name = tool.name,
                                arguments = tool.arguments.toString(),
                            )
                        )
                    }
                }
            }
        } catch (e: AnthropicServiceException) {
            emit(StreamEvent.Error("HTTP ${e.statusCode()}: ${e.message}"))
            return@flow
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            throw e
        } finally {
            cancelHandle?.dispose()
        }

        val message = accumulator.message()
        ReplayCache.remember(message)

        if (message.stopReason().orElse(null) == StopReason.REFUSAL && toolArgs.isEmpty()) {
            emit(StreamEvent.Error("Claude declined this request."))
            return@flow
        }

        toolArgs.values.forEach { tool ->
            emit(
                StreamEvent.ToolCallComplete(
                    CompletedToolCall(tool.id, tool.name, tool.arguments.toString().ifBlank { "{}" })
                )
            )
        }

        val usage = message.usage()
        val cacheRead = usage.cacheReadInputTokens().orElse(0L)
        val cacheWrite = usage.cacheCreationInputTokens().orElse(0L)
        Log.i(TAG, "usage input=${usage.inputTokens()} cacheRead=$cacheRead cacheWrite=$cacheWrite output=${usage.outputTokens()}")
        emit(
            StreamEvent.Usage(
                TokenUsage(
                    promptTokens = (usage.inputTokens() + cacheRead + cacheWrite).toInt(),
                    completionTokens = usage.outputTokens().toInt(),
                    cachedTokens = cacheRead.toInt(),
                )
            )
        )
        emit(StreamEvent.Done)
    }.flowOn(Dispatchers.IO)

    private fun buildParams(messages: List<ChatMessage>, tools: JSONArray?): MessageCreateParams {
        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(MAX_TOKENS)
            // Adaptive thinking; if history was edited since a thinking block was produced
            // (compaction, system prompt change), drop that block instead of failing the request.
            .thinking(
                ThinkingConfigAdaptive.builder()
                    .putAdditionalProperty(
                        "block_binding",
                        JsonValue.from(mapOf("prefix_mismatch_behavior" to "drop_block")),
                    )
                    .build()
            )
            .putAdditionalHeader("anthropic-beta", THINKING_BINDING_BETA)
            .messages(toMessageParams(messages.filter { it.role != "system" }))
            // Automatic breakpoint on the last block: each turn reuses the previous turn's prefix.
            .cacheControl(CacheControlEphemeral.builder().build())

        val system = messages.filter { it.role == "system" }.mapNotNull { it.content?.takeIf(String::isNotBlank) }
        if (system.isNotEmpty()) {
            builder.systemOfTextBlockParams(
                listOf(
                    TextBlockParam.builder()
                        .text(system.joinToString("\n\n"))
                        // Caches tools + system, which are identical on every turn.
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()
                )
            )
        }
        tools?.let { builder.tools(toTools(it)) }
        return builder.build()
    }

    private fun toMessageParams(messages: List<ChatMessage>): List<MessageParam> {
        val turns = mutableListOf<Turn>()
        fun turnFor(role: MessageParam.Role): Turn =
            turns.lastOrNull()?.takeIf { it.role == role } ?: Turn(role).also { turns += it }

        for (message in messages) {
            when (message.role) {
                "assistant" -> {
                    val replayed = ReplayCache.lookup(message)
                    val blocks = replayed ?: buildList {
                        message.content?.takeIf(String::isNotBlank)?.let { add(textBlock(it)) }
                        message.toolCalls.forEach { call ->
                            add(
                                ContentBlockParam.ofToolUse(
                                    ToolUseBlockParam.builder()
                                        .id(call.id)
                                        .name(call.name)
                                        .input(toolInput(call.arguments))
                                        .build()
                                )
                            )
                        }
                    }
                    if (blocks.isNotEmpty()) turnFor(MessageParam.Role.ASSISTANT).blocks += blocks
                }
                "tool" -> {
                    val id = message.toolCallId ?: continue
                    turnFor(MessageParam.Role.USER).blocks += ContentBlockParam.ofToolResult(
                        ToolResultBlockParam.builder()
                            .toolUseId(id)
                            .content(message.content?.takeIf(String::isNotBlank) ?: "(no output)")
                            .build()
                    )
                }
                else -> {
                    val blocks = buildList {
                        message.content?.takeIf(String::isNotBlank)?.let { add(textBlock(it)) }
                        mediaBlock(message)?.let { add(it) }
                    }
                    if (blocks.isNotEmpty()) turnFor(MessageParam.Role.USER).blocks += blocks
                }
            }
        }

        repairToolPairing(turns)
        if (turns.firstOrNull()?.role != MessageParam.Role.USER) {
            turns.add(0, Turn(MessageParam.Role.USER).apply { blocks += textBlock("(conversation continues)") })
        }
        if (turns.last().role != MessageParam.Role.USER) {
            turns += Turn(MessageParam.Role.USER).apply { blocks += textBlock("Continue.") }
        }
        return turns.map { MessageParam.builder().role(it.role).contentOfBlockParams(it.blocks).build() }
    }

    /**
     * Every tool_use needs a tool_result in the next user turn, and a tool_result needs a matching
     * tool_use; interrupted runs or compaction can leave either side dangling, which is a 400.
     */
    private fun repairToolPairing(turns: MutableList<Turn>) {
        var i = 0
        while (i < turns.size) {
            val turn = turns[i]
            if (turn.role == MessageParam.Role.USER) {
                val allowed = turns.getOrNull(i - 1)?.toolUseIds().orEmpty()
                turn.blocks.removeAll { it.toolResult().orElse(null)?.let { r -> r.toolUseId() !in allowed } ?: false }
                if (turn.blocks.isEmpty()) {
                    turns.removeAt(i)
                    continue
                }
            } else {
                val ids = turn.toolUseIds()
                if (ids.isNotEmpty()) {
                    val next = turns.getOrNull(i + 1)?.takeIf { it.role == MessageParam.Role.USER }
                        ?: Turn(MessageParam.Role.USER).also { turns.add(i + 1, it) }
                    val answered = next.blocks.mapNotNull { it.toolResult().orElse(null)?.toolUseId() }.toSet()
                    val missing = ids.filter { it !in answered }.map { id ->
                        ContentBlockParam.ofToolResult(
                            ToolResultBlockParam.builder()
                                .toolUseId(id)
                                .content("Tool execution was interrupted before it returned a result.")
                                .isError(true)
                                .build()
                        )
                    }
                    next.blocks.addAll(0, missing)
                }
            }
            i++
        }
    }

    private fun Turn.toolUseIds(): Set<String> =
        if (role == MessageParam.Role.ASSISTANT) blocks.mapNotNull { it.toolUse().orElse(null)?.id() }.toSet() else emptySet()

    private fun mediaBlock(message: ChatMessage): ContentBlockParam? {
        val path = message.mediaPath ?: return null
        val mime = message.mediaMimeType ?: return null
        val file = File(path).takeIf { it.isFile } ?: return null
        val mediaType = when (mime.lowercase()) {
            "image/jpeg", "image/jpg" -> Base64ImageSource.MediaType.IMAGE_JPEG
            "image/png" -> Base64ImageSource.MediaType.IMAGE_PNG
            "image/gif" -> Base64ImageSource.MediaType.IMAGE_GIF
            "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP
            else -> return textBlock("[Attached file ${file.name} ($mime) cannot be viewed directly; it is saved at ${file.absolutePath}]")
        }
        return ContentBlockParam.ofImage(
            ImageBlockParam.builder()
                .source(
                    Base64ImageSource.builder()
                        .data(Base64.encodeToString(file.readBytes(), Base64.NO_WRAP))
                        .mediaType(mediaType)
                        .build()
                )
                .build()
        )
    }

    /** Converts OpenAI-style `{type: function, function: {...}}` tool JSON to Messages API tools. */
    private fun toTools(tools: JSONArray): List<ToolUnion> {
        val seen = mutableSetOf<String>()
        return (0 until tools.length()).mapNotNull { i ->
            val raw = tools.optJSONObject(i) ?: return@mapNotNull null
            val fn = raw.optJSONObject("function") ?: raw
            val name = fn.optString("name")
            if (!TOOL_NAME.matches(name) || !seen.add(name)) {
                Log.w(TAG, "Skipping tool with invalid or duplicate name: $name")
                return@mapNotNull null
            }
            val schema = fn.optJSONObject("parameters") ?: fn.optJSONObject("input_schema") ?: JSONObject()
            val inputSchema = Tool.InputSchema.builder().apply {
                schema.optJSONObject("properties")?.let { props ->
                    properties(
                        Tool.InputSchema.Properties.builder()
                            .putAllAdditionalProperties(props.keys().asSequence().associateWith { JsonValue.from(props.get(it).toPlain()) })
                            .build()
                    )
                }
                schema.optJSONArray("required")?.let { req -> required((0 until req.length()).map(req::getString)) }
                schema.keys().asSequence()
                    .filter { it !in setOf("type", "properties", "required") }
                    .forEach { putAdditionalProperty(it, JsonValue.from(schema.get(it).toPlain())) }
            }.build()
            ToolUnion.ofTool(
                Tool.builder()
                    .name(name)
                    .description(fn.optString("description"))
                    .inputSchema(inputSchema)
                    .build()
            )
        }
    }

    private fun toolInput(arguments: String): ToolUseBlockParam.Input {
        val parsed = runCatching { JSONObject(arguments.ifBlank { "{}" }) }.getOrNull()
        val map = parsed?.let { obj -> obj.keys().asSequence().associateWith { JsonValue.from(obj.get(it).toPlain()) } }
            ?: mapOf("raw_arguments" to JsonValue.from(arguments))
        return ToolUseBlockParam.Input.builder().putAllAdditionalProperties(map).build()
    }

    private fun textBlock(text: String) = ContentBlockParam.ofText(TextBlockParam.builder().text(text).build())

    private class Turn(val role: MessageParam.Role) {
        val blocks = mutableListOf<ContentBlockParam>()
    }

    private class ToolStream(val id: String, val name: String) {
        val arguments = StringBuilder()
    }

    /**
     * Holds recent assistant turns exactly as Claude returned them so thinking blocks (and their
     * signatures) can be sent back unchanged; the database only stores text and tool calls.
     */
    private object ReplayCache {
        private const val MAX_ENTRIES = 64
        private val turns = object : LinkedHashMap<String, List<ContentBlockParam>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ContentBlockParam>>?) = size > MAX_ENTRIES
        }

        @Synchronized
        fun remember(message: Message) {
            val blocks = message.toParam().content().blockParams().orElse(null) ?: return
            val firstToolId = message.content().firstNotNullOfOrNull { it.toolUse().orElse(null)?.id() }
            val text = message.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
            turns[key(firstToolId, text)] = blocks
        }

        @Synchronized
        fun lookup(message: ChatMessage): List<ContentBlockParam>? =
            turns[key(message.toolCalls.firstOrNull()?.id, message.content.orEmpty())]

        private fun key(firstToolId: String?, text: String) = firstToolId ?: "text:${text.trim().hashCode()}"
    }

    companion object {
        private const val TAG = "LlmApiClient"
        private const val MAX_TOKENS = 32_000L
        private const val THINKING_BINDING_BETA = "thinking-binding-controls-2026-08-01"
        private val TOOL_NAME = Regex("^[a-zA-Z0-9_-]{1,128}$")

        @Volatile private var cached: Triple<String, String, AnthropicClient>? = null

        /** One shared client (and connection pool) per endpoint + key. */
        private fun client(baseUrl: String, apiKey: String): AnthropicClient {
            val root = baseUrl.trimEnd('/').removeSuffix("/v1")
            cached?.let { (url, key, client) -> if (url == root && key == apiKey) return client }
            return synchronized(this) {
                cached?.takeIf { it.first == root && it.second == apiKey }?.third
                    ?: AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .baseUrl(root)
                        .timeout(Duration.ofMinutes(10))
                        .build()
                        .also { cached = Triple(root, apiKey, it) }
            }
        }
    }
}

private fun Any?.toPlain(): Any? = when (this) {
    is JSONObject -> keys().asSequence().associateWith { get(it).toPlain() }
    is JSONArray -> (0 until length()).map { get(it).toPlain() }
    JSONObject.NULL -> null
    else -> this
}
