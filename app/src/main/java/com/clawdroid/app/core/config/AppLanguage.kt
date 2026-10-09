package com.clawdroid.app.core.config

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language choice. Defaults to Simplified Chinese; "system" follows the device.
 * On Android 13+ this uses the per-app language API so services and notifications
 * follow too; older versions wrap the activity context instead.
 */
object AppLanguage {
    const val ZH = "zh-CN"
    const val EN = "en"
    const val SYSTEM = "system"

    private const val PREFS = "clawdroid_config"
    private const val KEY = "app_language"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, ZH) ?: ZH

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value).apply()
        apply(context)
    }

    /** Pushes the saved choice to the system per-app locale (Android 13+). */
    fun apply(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        val wanted = when (val lang = get(context)) {
            SYSTEM -> LocaleList.getEmptyLocaleList()
            else -> LocaleList.forLanguageTags(lang)
        }
        if (manager.applicationLocales != wanted) manager.applicationLocales = wanted
    }

    /**
     * Returns a context whose resources use the chosen locale. Needed before Android 13, and
     * also on 13+ for the first launch after an install, which clears the per-app locale
     * until [apply] restores it.
     */
    fun wrap(base: Context): Context {
        val lang = get(base)
        if (lang == SYSTEM) return base
        val locale = Locale.forLanguageTag(lang)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    /** Locale the app is using (works from application context on every API level). */
    fun current(context: Context): Locale = when (val lang = get(context)) {
        SYSTEM -> Locale.getDefault()
        else -> Locale.forLanguageTag(lang)
    }
}
