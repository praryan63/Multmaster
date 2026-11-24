package com.multmaster.app.data

import com.multmaster.app.domain.Question
import com.multmaster.app.domain.SpacedRepetition
import kotlinx.coroutines.flow.Flow

class MistakeRepository(private val dao: MistakeDao) {

    fun active(): Flow<List<MistakeEntity>> = dao.activeFlow()
    fun resolved(): Flow<List<MistakeEntity>> = dao.resolvedFlow()
    fun all(): Flow<List<MistakeEntity>> = dao.allFlow()
    suspend fun activeList(): List<MistakeEntity> = dao.activeList()

    suspend fun onWrong(q: Question, given: Int) {
        val now = System.currentTimeMillis()
        val existing = dao.find(q.a, q.b)
        if (existing == null) {
            dao.insertIgnore(
                MistakeEntity(a = q.a, b = q.b, correct = q.answer, lastGiven = given,
                    createdAt = now, lastAskedAt = now, dueAt = now)
            )
        } else {
            dao.update(
                existing.copy(
                    mistakeCount = existing.mistakeCount + 1,
                    lastGiven = given, lastAskedAt = now, dueAt = now,
                    intervalIndex = 0, consecutiveCorrect = 0, resolved = false
                )
            )
        }
    }

    suspend fun onCorrectRevision(q: Question) {
        val now = System.currentTimeMillis()
        val existing = dao.find(q.a, q.b) ?: return
        val nextIndex = (existing.intervalIndex + 1).coerceAtMost(SpacedRepetition.intervalsDays.lastIndex)
        val streak = existing.consecutiveCorrect + 1
        val resolved = nextIndex >= SpacedRepetition.intervalsDays.lastIndex && streak >= 2
        dao.update(
            existing.copy(
                intervalIndex = nextIndex,
                lastAskedAt = now,
                dueAt = SpacedRepetition.dueAfter(nextIndex, now),
                consecutiveCorrect = streak,
                resolved = resolved
            )
        )
    }

    suspend fun setBookmarked(id: Long, value: Boolean) {
        val list = dao.all()
        list.firstOrNull { it.id == id }?.let { dao.update(it.copy(bookmarked = value)) }
    }
}