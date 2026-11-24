package com.multmaster.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val a: Int,
    val b: Int,
    val correct: Int,
    val given: Int?,
    val isCorrect: Boolean,
    val mode: String,
    val responseMs: Long,
    val timestamp: Long,
    val sessionId: Long
)

@Serializable
@Entity(tableName = "mistakes", indices = [Index(value = ["a", "b"], unique = true)])
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val a: Int,
    val b: Int,
    val correct: Int,
    val lastGiven: Int,
    val mistakeCount: Int = 1,
    val createdAt: Long,
    val lastAskedAt: Long,
    val dueAt: Long,
    val intervalIndex: Int = 0,
    val consecutiveCorrect: Int = 0,
    val resolved: Boolean = false,
    val bookmarked: Boolean = false
)

@Serializable
@Entity(tableName = "table_progress")
data class TableProgressEntity(
    @PrimaryKey val tableNo: Int,
    val attempts: Int = 0,
    val correct: Int = 0,
    val mistakeCount: Int = 0,
    val consecutiveCorrect: Int = 0,
    val bestStreak: Int = 0,
    val learned: Boolean = false,
    val goodSessions: Int = 0,
    val lastPracticedAt: Long = 0
)

@Serializable
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val startedAt: Long,
    val endedAt: Long,
    val total: Int,
    val correct: Int,
    val totalMs: Long,
    val fastestMs: Long
)

@Serializable
@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val tableNo: Int)

data class TableAccuracy(val tableNo: Int, val total: Int, val correct: Int) {
    val accuracy: Double get() = if (total == 0) 0.0 else correct.toDouble() / total
}

data class DayStat(val date: String, val total: Int, val correct: Int)

data class Totals(val attempted: Int, val correct: Int)