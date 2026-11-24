package com.multmaster.app.domain

import kotlin.random.Random

enum class QuestionKind { DIRECT, MISSING_MULTIPLIER, MISSING_TABLE, DIVISION }

data class Question(
    val a: Int,                                   // table number
    val b: Int,                                   // multiplier
    val kind: QuestionKind = QuestionKind.DIRECT
) {
    val answer: Int
        get() = when (kind) {
            QuestionKind.MISSING_MULTIPLIER, QuestionKind.DIVISION -> b
            QuestionKind.MISSING_TABLE -> a
            QuestionKind.DIRECT -> a * b
        }

    val prompt: String
        get() = when (kind) {
            QuestionKind.DIRECT -> "$a × $b = ?"
            QuestionKind.MISSING_MULTIPLIER -> "$a × ? = ${a * b}"
            QuestionKind.MISSING_TABLE -> "? × $b = ${a * b}"
            QuestionKind.DIVISION -> "${a * b} ÷ $a = ?"
        }

    val explanation: String
        get() = when (kind) {
            QuestionKind.DIVISION -> "${a * b} ÷ $a = $b   (${a} × $b = ${a * b})"
            else -> "$a × $b = ${a * b}"
        }

    val tableNo: Int get() = a
}

enum class PracticeMode {
    SEQUENTIAL, RANDOM, MIXED, REVERSE, MISSING, SPEED, MISTAKE, CUSTOM, MENTAL, CHALLENGE
}

data class PracticeConfig(
    val mode: PracticeMode,
    val tables: List<Int> = (1..50).toList(),
    val multiplierRange: IntRange = 1..20,
    val count: Int = 20,
    val timerSeconds: Int? = null,
    val sequential: Boolean = false,
    val instantFeedback: Boolean = true,
    val onlyWeak: Boolean = false,
    val explicitQuestions: List<Question>? = null,
    val title: String = ""
)

data class ResultItem(
    val question: Question,
    val given: Int?,
    val isCorrect: Boolean,
    val ms: Long
)