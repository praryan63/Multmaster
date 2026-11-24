package com.multmaster.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Backup(
    val version: Int = 1,
    val attempts: List<AttemptEntity> = emptyList(),
    val mistakes: List<MistakeEntity> = emptyList(),
    val tables: List<TableProgressEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val favorites: List<Int> = emptyList()
)

class BackupRepository(private val db: AppDatabase) {

    suspend fun export(): String {
        val backup = Backup(
            attempts = db.attemptDao().all(),
            mistakes = db.mistakeDao().all(),
            tables = db.tableProgressDao().all(),
            sessions = db.sessionDao().all(),
            favorites = db.favoriteDao().all()
        )
        return Json { prettyPrint = true }.encodeToString(Backup.serializer(), backup)
    }

    suspend fun import(json: String): Result<Unit> = runCatching {
        val b = Json { ignoreUnknownKeys = true }.decodeFromString(Backup.serializer(), json)
        db.attemptDao().clear()
        db.mistakeDao().clear()
        db.tableProgressDao().clear()
        db.sessionDao().clear()
        db.favoriteDao().clear()
        db.attemptDao().insertAll(b.attempts)
        db.mistakeDao().insertAll(b.mistakes)
        db.tableProgressDao().insertAll(b.tables)
        db.sessionDao().insertAll(b.sessions)
        db.favoriteDao().insertAll(b.favorites.map { FavoriteEntity(it) })
    }

    suspend fun resetAll() {
        db.attemptDao().clear()
        db.mistakeDao().clear()
        db.tableProgressDao().clear()
        db.sessionDao().clear()
    }
}