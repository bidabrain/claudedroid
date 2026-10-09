package com.clawdroid.app.core.config

import android.content.Context
import java.io.File

/**
 * One-time migration from the old branding (ClawDroid, agent name "Nova") to
 * ClaudeDroid / "Claude", covering saved settings and the agent's on-disk
 * workspace and memory files that get injected into the prompt.
 */
object RenameMigration {
    private const val PREFS = "clawdroid_config"
    private const val KEY_DONE = "migrated_to_claudedroid_v1"

    fun run(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return

        if (prefs.getString("agent_name", null) == "Nova") {
            prefs.edit().putString("agent_name", "Claude").apply()
        }

        val home = File(context.filesDir, "home")
        listOf(home, File(home, "workspace"), File(home, ".memory"))
            .flatMap { dir -> dir.listFiles { f -> f.isFile && f.name.endsWith(".md") }.orEmpty().toList() }
            .forEach { file -> runCatching { migrateFile(file) } }

        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    private fun migrateFile(file: File) {
        val old = file.readText()
        val new = old
            .replace("You are ClawDroid:", "You are Claude, the agent inside the ClaudeDroid app:")
            .replace(Regex("(?m)^Name: ClawDroid$"), "Name: Claude")
            .replace(Regex("(\\*\\*(?:Agent )?Name:\\*\\*) Nova\\b"), "$1 Claude")
            .replace(Regex("\\bClawDroid\\b"), "ClaudeDroid")
            .replace(Regex("\\bOpenClaw\\b"), "ClaudeDroid")
        if (new != old) file.writeText(new)
    }
}
