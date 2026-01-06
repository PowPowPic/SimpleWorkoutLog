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
}
