package com.multmaster.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(a: AttemptEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<AttemptEntity>)
    @Query("SELECT * FROM attempts") suspend fun all(): List<AttemptEntity>
    @Query("DELETE FROM attempts") suspend fun clear()

    @Query("SELECT COUNT(*) FROM attempts") fun totalFlow(): Flow<Int>
    @Query("SELECT COUNT(*) FROM attempts WHERE isCorrect = 1") fun correctFlow(): Flow<Int>
    @Query("SELECT COALESCE(AVG(responseMs),0.0) FROM attempts") fun avgMsFlow(): Flow<Double>
    @Query("SELECT COALESCE(MIN(responseMs),0) FROM attempts WHERE isCorrect = 1") fun fastestFlow(): Flow<Long>

    @Query("""SELECT a AS tableNo, COUNT(*) AS total, COALESCE(SUM(isCorrect),0) AS correct
              FROM attempts GROUP BY a""")
    fun perTableFlow(): Flow<List<TableAccuracy>>

    @Query("""SELECT strftime('%Y-%m-%d', timestamp/1000, 'unixepoch', 'localtime') AS date,
                     COUNT(*) AS total, COALESCE(SUM(isCorrect),0) AS correct
              FROM attempts
              GROUP BY date ORDER BY date ASC""")
    fun dailyFlow(): Flow<List<DayStat>>
}

@Dao
interface MistakeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(m: MistakeEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<MistakeEntity>)
    @Update suspend fun update(m: MistakeEntity)
    @Query("SELECT * FROM mistakes WHERE a = :a AND b = :b LIMIT 1") suspend fun find(a: Int, b: Int): MistakeEntity?
    @Query("SELECT * FROM mistakes WHERE resolved = 0 ORDER BY dueAt ASC") fun activeFlow(): Flow<List<MistakeEntity>>
    @Query("SELECT * FROM mistakes WHERE resolved = 1 ORDER BY mistakeCount DESC") fun resolvedFlow(): Flow<List<MistakeEntity>>
    @Query("SELECT * FROM mistakes ORDER BY mistakeCount DESC") fun allFlow(): Flow<List<MistakeEntity>>
    @Query("SELECT * FROM mistakes WHERE resolved = 0 ORDER BY dueAt ASC") suspend fun activeList(): List<MistakeEntity>
    @Query("SELECT * FROM mistakes") suspend fun all(): List<MistakeEntity>
    @Query("DELETE FROM mistakes") suspend fun clear()
}

@Dao
interface TableProgressDao {
    @Query("SELECT * FROM table_progress") fun allFlow(): Flow<List<TableProgressEntity>>
    @Query("SELECT * FROM table_progress") suspend fun all(): List<TableProgressEntity>
    @Query("SELECT * FROM table_progress WHERE tableNo = :t") suspend fun get(t: Int): TableProgressEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: TableProgressEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<TableProgressEntity>)
    @Query("DELETE FROM table_progress") suspend fun clear()
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(s: SessionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<SessionEntity>)
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC LIMIT :limit") fun recentFlow(limit: Int): Flow<List<SessionEntity>>
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC") suspend fun all(): List<SessionEntity>
    @Query("DELETE FROM sessions") suspend fun clear()
}

@Dao
interface FavoriteDao {
    @Query("SELECT tableNo FROM favorites") fun flow(): Flow<List<Int>>
    @Query("SELECT tableNo FROM favorites") suspend fun all(): List<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(f: FavoriteEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<FavoriteEntity>)
    @Query("DELETE FROM favorites WHERE tableNo = :t") suspend fun delete(t: Int)
    @Query("DELETE FROM favorites") suspend fun clear()
}