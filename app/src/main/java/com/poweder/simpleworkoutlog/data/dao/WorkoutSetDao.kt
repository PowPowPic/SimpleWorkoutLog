package com.poweder.simpleworkoutlog.data.dao

import androidx.room.*
import com.poweder.simpleworkoutlog.data.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setNumber")
    fun getSetsBySession(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getSetById(id: Long): WorkoutSetEntity?

    @Query("SELECT MAX(setNumber) FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun getMaxSetNumber(sessionId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: WorkoutSetEntity): Long

    @Update
    suspend fun update(set: WorkoutSetEntity)

    @Delete
    suspend fun delete(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAll()

    @Query("SELECT SUM(totalWeight) FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun getTotalWeightBySession(sessionId: Long): Double?

    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.sessionId = s.id
        WHERE s.logicalDate = :date
    """)
    fun getSetsForDate(date: Long): Flow<List<WorkoutSetEntity>>

    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.sessionId = s.id
        WHERE s.logicalDate BETWEEN :startDate AND :endDate
    """)
    fun getSetsBetween(startDate: Long, endDate: Long): Flow<List<WorkoutSetEntity>>

    /**
     * 指定種目の日別MAX weightを取得（グラフ用）
     * 記録がある日のみ返す
     */
    @Query("""
        SELECT s.logicalDate as date, MAX(ws.weight) as maxWeight
        FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.sessionId = s.id
        WHERE s.exerciseId = :exerciseId
        AND s.logicalDate >= :startDate
        GROUP BY s.logicalDate
        ORDER BY s.logicalDate ASC
    """)
    fun getDailyMaxWeightForExercise(exerciseId: Long, startDate: Long): Flow<List<DailyMaxWeight>>
}

/**
 * 日別MAX weightのデータクラス
 */
data class DailyMaxWeight(
    val date: Long,
    val maxWeight: Double
)