package com.clawdroid.app.data.api

import com.clawdroid.app.core.control.ScreenReaderService
import com.clawdroid.app.data.db.ConversationDao
import com.clawdroid.app.data.db.MessageDao
import com.clawdroid.app.data.db.MessageEntity
import com.clawdroid.app.data.db.ToolCallDao
import com.clawdroid.app.data.db.ToolCallEntity
import java.util.UUID

/**
 * Builds the list of [ChatMessage] to send to the LLM for a given conversation.
 *
 * Context structure:
 * ```
 * [System prompt]
 * [Compaction summary as "user" message]  ← if conversation.summaryMessageId is set
 * [Messages after summary point]          ← from Room DB
 * ```
 */
import android.content.Context

class ContextBuilder(
    private val context: Context,
    private val projectId: String?,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val toolCallDao: ToolCallDao,
) {
    /**
     * Build the full message list for an LLM call.
     *
     * @param conversationId the conversation to build context for
     * @param includeSystemPrompt whether to prepend the system prompt (true for normal calls, false for compaction)
     */
    suspend fun buildContext(
        conversationId: String,
        includeSystemPrompt: Boolean = true,
    ): List<ChatMessage> {
        val conversation = conversationDao.getById(conversationId)
            ?: error("Conversation $conversationId not found")

        val allMessages = messageDao.getAll(conversationId)

        val result = mutableListOf<ChatMessage>()

        // 1. System prompt
        if (includeSystemPrompt) {
            result += ChatMessage(role = "system", content = systemPromptFor(conversationId))
        }

        // 2. Determine start index based on compaction
        val startIndex = if (conversation.summaryMessageId != null) {
            val summaryIdx = allMessages.indexOfFirst { it.id == conversation.summaryMessageId }
            if (summaryIdx >= 0) {
                // Inject summary as a "user" message (like OpenCode does)
                val summaryMsg = allMessages[summaryIdx]
                result += ChatMessage(
                    role = "user",
                    content = "Previous conversation summary:\n${summaryMsg.content}",
                )
                summaryIdx + 1 // Skip the summary message itself
            } else {
                0 // Summary message not found, use all messages
            }
        } else {
            0
        }

        // 3. Convert remaining messages to ChatMessage format
        for (i in startIndex until allMessages.size) {
            val msg = allMessages[i]
            val toolCalls = if (msg.role == "assistant") {
                toolCallDao.getForMessage(msg.id).map { entity ->
                    CompletedToolCall(
                        id = entity.id,
                        name = entity.toolName,
                        arguments = entity.arguments
                    )
                }
            } else {
                emptyList()
            }
            result += msg.toChatMessage(toolCalls)
        }

        return result
    }

    /**
     * Save a user message to the database and return its ID.
     */
    suspend fun saveUserMessage(
        conversationId: String,
        content: String,
        mediaPath: String? = null,
        mediaMimeType: String? = null,
    ): String {
        val id = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = id,
                conversationId = conversationId,
                role = "user",
                content = content,
                createdAt = System.currentTimeMillis(),
                tokenCount = (content.length / 4).coerceAtLeast(1),
                mediaPath = mediaPath,
                mediaMimeType = mediaMimeType,
            )
        )
        return id
    }

    /**
     * Save an assistant message (possibly with tool calls) to the database.
     */
    suspend fun saveAssistantMessage(
        conversationId: String,
        content: String,
        toolCalls: List<CompletedToolCall> = emptyList(),
    ): String {
        val id = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = id,
                conversationId = conversationId,
                role = "assistant",
                content = content,
                createdAt = System.currentTimeMillis(),
                tokenCount = (content.length / 4).coerceAtLeast(1),
            )
        )
        // Save tool calls to the separate table
        for (tc in toolCalls) {
            toolCallDao.upsert(
                ToolCallEntity(
                    id = tc.id,
                    messageId = id,
                    toolName = tc.name,
                    arguments = tc.arguments,
                    result = null,
                    status = "pending",
                    durationMs = 0,
                )
            )
        }
        return id
    }

    /**
     * Save a tool result message to the database.
     */
    suspend fun saveToolResultMessage(
        conversationId: String,
        toolCallId: String,
        content: String,
        isError: Boolean = false,
    ): String {
        val id = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = id,
                conversationId = conversationId,
                role = "tool",
                content = content,
                createdAt = System.currentTimeMillis(),
                tokenCount = (content.length / 4).coerceAtLeast(1),
                toolCallId = toolCallId,
            )
        )
        // Update the tool call record with the result
        toolCallDao.complete(
            id = toolCallId,
            result = content,
            status = if (isError) "error" else "completed",
            durationMs = 0,
        )
        return id
    }

    fun buildSystemPrompt(): String {
        return MessageBuilder.buildSystemPrompt(context, projectId)
    }

    /**
     * The system prompt is the start of the prompt-cache prefix, so it is frozen for the lifetime of
     * a conversation: memory appended after each run (and other edits) reach the next conversation
     * instead of invalidating this one's cache on every message. Toggling screen control still
     * rebuilds it because that section changes which tools the agent should reach for.
     */
    private fun systemPromptFor(conversationId: String): String {
        val key = "$conversationId|${ScreenReaderService.instance != null}"
        return synchronized(systemPrompts) {
            systemPrompts.getOrPut(key) { buildSystemPrompt() }
        }
    }

    private companion object {
        private val systemPrompts = object : LinkedHashMap<String, String>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 32
        }
    }
}

/**
 * Convert a [MessageEntity] to a [ChatMessage] for the LLM API.
 */
fun MessageEntity.toChatMessage(toolCalls: List<CompletedToolCall> = emptyList()): ChatMessage = ChatMessage(
    role = role,
    content = content.takeIf { it.isNotBlank() },
    toolCallId = toolCallId,
    toolCalls = toolCalls,
    mediaPath = mediaPath,
    mediaMimeType = mediaMimeType,
)
