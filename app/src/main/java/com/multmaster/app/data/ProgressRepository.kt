package com.multmaster.app.data

import com.multmaster.app.domain.PracticeMode
import com.multmaster.app.domain.Question
import com.multmaster.app.domain.ResultItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ProgressRepository(
    private val attemptDao: AttemptDao,
    private val tableDao: TableProgressDao,
    private val sessionDao: SessionDao
) {
    fun perTable(): Flow<List<TableAccuracy>> = attemptDao.perTableFlow()
    fun daily(): Flow<List<DayStat>> = attemptDao.dailyFlow()
    fun avgMs(): Flow<Double> = attemptDao.avgMsFlow()
    fun fastest(): Flow<Long> = attemptDao.fastestFlow()
    fun totals(): Flow<Totals> = combine(attemptDao.totalFlow(), attemptDao.correctFlow()) { a, c -> Totals(a, c) }
    fun sessions(limit: Int = 50): Flow<List<SessionEntity>> = sessionDao.recentFlow(limit)
    fun tableProgress(): Flow<List<TableProgressEntity>> = tableDao.allFlow()

    suspend fun recordAttempt(q: Question, given: Int?, ms: Long, mode: PracticeMode, sessionId: Long): Boolean {
        val now = System.currentTimeMillis()
        val isCorrect = given != null && given == q.answer
        attemptDao.insert(
            AttemptEntity(a = q.a, b = q.b, correct = q.answer, given = given, isCorrect = isCorrect,
                mode = mode.name, responseMs = ms, timestamp = now, sessionId = sessionId)
        )
        val cur = tableDao.get(q.a) ?: TableProgressEntity(tableNo = q.a)
        val streak = if (isCorrect) cur.consecutiveCorrect + 1 else 0
        tableDao.upsert(
            cur.copy(
                attempts = cur.attempts + 1,
                correct = cur.correct + if (isCorrect) 1 else 0,
                mistakeCount = cur.mistakeCount + if (isCorrect) 0 else 1,
                consecutiveCorrect = streak,
                bestStreak = maxOf(cur.bestStreak, streak),
                lastPracticedAt = now
            )
        )
        return isCorrect
    }

    suspend fun finishSession(sessionId: Long, mode: PracticeMode, startedAt: Long, results: List<ResultItem>) {
        if (results.isEmpty()) return
        val now = System.currentTimeMillis()
        val correct = results.count { it.isCorrect }
        sessionDao.insert(
            SessionEntity(
                mode = mode.name, startedAt = startedAt, endedAt = now,
                total = results.size, correct = correct,
                totalMs = results.sumOf { it.ms },
                fastestMs = results.filter { it.isCorrect }.minOfOrNull { it.ms } ?: 0
            )
        )
        results.groupBy { it.question.a }.forEach { (table, list) ->
            if (list.size >= 8) {
                val acc = list.count { it.isCorrect }.toDouble() / list.size
                val p = tableDao.get(table) ?: TableProgressEntity(tableNo = table)
                if (acc >= 0.9) tableDao.upsert(p.copy(goodSessions = p.goodSessions + 1))
            }
        }
    }

    suspend fun setLearned(table: Int, learned: Boolean) {
        val p = tableDao.get(table) ?: TableProgressEntity(tableNo = table)
        tableDao.upsert(p.copy(learned = learned))
    }
}