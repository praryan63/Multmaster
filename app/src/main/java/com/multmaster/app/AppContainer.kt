package com.multmaster.app

import android.content.Context
import com.multmaster.app.data.*
import com.multmaster.app.domain.PracticeConfig
import com.multmaster.app.util.SoundController
import com.multmaster.app.util.TtsController

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val db: AppDatabase by lazy { AppDatabase.get(appContext) }
    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }
    val progressRepo: ProgressRepository by lazy {
        ProgressRepository(db.attemptDao(), db.tableProgressDao(), db.sessionDao())
    }
    val mistakeRepo: MistakeRepository by lazy { MistakeRepository(db.mistakeDao()) }
    val backupRepo: BackupRepository by lazy { BackupRepository(db) }
    val tts: TtsController by lazy { TtsController(appContext) }
    val sound: SoundController by lazy { SoundController() }

    /** Set immediately before navigating to the practice screen. */
    var pendingConfig: PracticeConfig? = null
}