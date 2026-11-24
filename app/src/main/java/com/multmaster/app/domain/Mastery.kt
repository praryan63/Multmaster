package com.multmaster.app.domain

import com.multmaster.app.data.TableProgressEntity

enum class MasteryLevel { NOT_STARTED, LEARNING, PRACTICING, IMPROVING, MASTERED }

object Mastery {
    fun of(p: TableProgressEntity): MasteryLevel {
        if (p.attempts == 0) return MasteryLevel.NOT_STARTED
        val accuracy = p.correct.toDouble() / p.attempts
        return when {
            p.learned && p.attempts >= 40 && accuracy >= 0.90 && p.goodSessions >= 2 -> MasteryLevel.MASTERED
            p.attempts >= 25 && accuracy >= 0.80 && p.goodSessions >= 1 -> MasteryLevel.IMPROVING
            p.attempts >= 10 -> MasteryLevel.PRACTICING
            else -> MasteryLevel.LEARNING
        }
    }

    fun isWeak(p: TableProgressEntity): Boolean {
        if (p.attempts < 5) return false
        return p.correct.toDouble() / p.attempts < 0.75
    }
}