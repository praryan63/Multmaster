package com.multmaster.app.domain

import kotlin.random.Random

object QuestionGenerator {

    fun build(config: PracticeConfig, mistakePool: List<Pair<Int, Int>> = emptyList(),
              random: Random = Random.Default): List<Question> {
        config.explicitQuestions?.let { if (it.isNotEmpty()) return it }

        val tables = config.tables.ifEmpty { (1..50).toList() }
        val multipliers = config.multiplierRange.toList().ifEmpty { (1..20).toList() }
        val count = config.count.coerceIn(1, 200)

        if (config.mode == PracticeMode.MISTAKE) {
            if (mistakePool.isEmpty()) return emptyList()
            val list = ArrayList<Question>(count)
            var guard = 0
            while (list.size < count && guard < count * 20) {
                guard++
                val p = mistakePool[random.nextInt(mistakePool.size)]
                list += Question(p.first, p.second)
            }
            return list.shuffled(random)
        }

        if (config.mode == PracticeMode.SEQUENTIAL || config.sequential) {
            val table = tables.first()
            return (0 until count).map { Question(table, multipliers[it % multipliers.size]) }
        }

        val out = ArrayList<Question>(count)
        var lastA = -1
        var lastB = -1
        var guard = 0
        while (out.size < count && guard < count * 40) {
            guard++
            val a = tables[random.nextInt(tables.size)]
            val b = multipliers[random.nextInt(multipliers.size)]
            if (a == lastA && b == lastB) continue
            val kind = pickKind(config, random)
            out += Question(a, b, kind)
            lastA = a; lastB = b
        }
        return out
    }

    private fun pickKind(config: PracticeConfig, random: Random): QuestionKind {
        return when (config.mode) {
            PracticeMode.REVERSE ->
                if (random.nextBoolean()) QuestionKind.DIVISION else QuestionKind.MISSING_MULTIPLIER
            PracticeMode.MISSING ->
                if (random.nextBoolean()) QuestionKind.MISSING_MULTIPLIER else QuestionKind.MISSING_TABLE
            PracticeMode.MIXED, PracticeMode.CUSTOM ->
                when (random.nextInt(10)) {
                    0 -> QuestionKind.MISSING_MULTIPLIER
                    1 -> QuestionKind.DIVISION
                    2 -> QuestionKind.MISSING_TABLE
                    else -> QuestionKind.DIRECT
                }
            else -> QuestionKind.DIRECT
        }
    }

    /** Deterministic question of the day — the same question all day long. */
    fun dailyQuestion(dayIndex: Long): Question {
        val r = Random(dayIndex * 7919L + 13)
        return Question(r.nextInt(2, 21), r.nextInt(2, 21))
    }
}