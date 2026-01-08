package com.poweder.simpleworkoutlog.data.dao

import androidx.room.*
import com.poweder.simpleworkoutlog.data.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE logicalDate = :logicalDate ORDER BY createdAt")
    fun getSessionsByDate(logicalDate: Long): Flow<List<WorkoutSessionEntity>>
    
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSessionEntity?
    
    @Query("SELECT * FROM workout_sessions WHERE exerciseId = :exerciseId AND logicalDate = :logicalDate")
    suspend fun getSessionByExerciseAndDate(exerciseId: Long, logicalDate: Long): WorkoutSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSessionEntity): Long
    
    @Update
    suspend fun update(session: WorkoutSessionEntity)
    
    @Delete
    suspend fun delete(session: WorkoutSessionEntity)
    
    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM workout_sessions WHERE logicalDate = :logicalDate")
    suspend fun deleteByDate(logicalDate: Long)
    
    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT logicalDate FROM workout_sessions WHERE logicalDate BETWEEN :startDate AND :endDate ORDER BY logicalDate")
    fun getSessionDatesBetween(startDate: Long, endDate: Long): Flow<List<Long>>

    @Query("SELECT * FROM workout_sessions WHERE logicalDate BETWEEN :startDate AND :endDate ORDER BY logicalDate, createdAt")
    fun getSessionsBetween(startDate: Long, endDate: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT MIN(logicalDate) FROM workout_sessions")
    fun getOldestSessionDate(): Flow<Long?>
}
