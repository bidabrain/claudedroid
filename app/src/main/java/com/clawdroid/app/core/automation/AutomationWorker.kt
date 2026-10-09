package com.clawdroid.app.core.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clawdroid.app.data.db.ClawDroidDatabase

import com.clawdroid.app.core.config.AppConfigManager
import com.clawdroid.app.core.engine.BackgroundAgentRunner
import com.clawdroid.app.data.db.ConversationEntity
import com.clawdroid.app.core.service.EnhancedForegroundService
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import java.io.File
import com.clawdroid.app.R

class AutomationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // Self-heal EnhancedForegroundService if Ultra Agent is enabled
        if (AppConfigManager.ultraAgentEnabled) {
            runCatching {
                val serviceIntent = Intent(applicationContext, EnhancedForegroundService::class.java)
                ContextCompat.startForegroundService(applicationContext, serviceIntent)
            }
        }

        if (AppConfigManager.heartbeatEnabled) {
            runCatching {
                val db = ClawDroidDatabase.get(applicationContext)
                val projects = db.projects().observeProjects().first()
                val homeDir = File(applicationContext.filesDir, "home")

                for (project in projects) {
                    val projectDir = File(homeDir, "projects/${project.id}")
                    val heartbeatFile = File(projectDir, "heartbeat.md").takeIf { it.exists() }
                        ?: File(projectDir, "HEARTBEAT.md").takeIf { it.exists() }

                    if (heartbeatFile != null) {
                        val heartbeatContent = heartbeatFile.readText().trim()
                        if (heartbeatContent.isNotBlank()) {
                            val existingList = db.conversations().observeForProject(project.id).first()
                            val heartbeatTitle = applicationContext.getString(R.string.general_heartbeat_chat_title)
                            val existing = existingList.firstOrNull {
                                it.id == "heartbeat_chat_${project.id}" || it.title == "Autonomous Heartbeat" || it.title == heartbeatTitle
                            }
                            val conversationId = if (existing != null) {
                                existing.id
                            } else {
                                val newId = "heartbeat_chat_${project.id}"
                                db.conversations().upsert(
                                    ConversationEntity(
                                        id = newId,
                                        projectId = project.id,
                                        title = heartbeatTitle,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis(),
                                        status = "idle",
                                        costUsd = 0.0
                                    )
                                )
                                newId
                            }

                            val prompt = "Autonomous Heartbeat Check. Scan your heartbeat.md tasks and execute necessary checks:\n\n$heartbeatContent\n\nIf you perform actions, use send_notification to inform the user."
                            BackgroundAgentRunner.runAgentInBackground(
                                context = applicationContext,
                                projectId = project.id,
                                conversationId = conversationId,
                                prompt = prompt
                            )
                        }
                    }
                }
            }
        }

        return Result.success()
    }
}
