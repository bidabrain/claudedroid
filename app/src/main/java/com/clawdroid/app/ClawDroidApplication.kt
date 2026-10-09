package com.clawdroid.app

import android.app.Application
import com.clawdroid.app.core.config.AppConfigManager
import com.clawdroid.app.core.config.AppLanguage
import com.clawdroid.app.core.config.RenameMigration

class ClawDroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RenameMigration.run(this)
        AppConfigManager.init(this)
        AppLanguage.apply(this)
    }
}
