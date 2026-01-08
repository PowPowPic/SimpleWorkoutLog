package com.poweder.simpleworkoutlog.data.dao

import androidx.room.*
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY sortOrder, id")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE workoutType = :type ORDER BY sortOrder, id")
    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()

    @Query("SELECT MAX(sortOrder) FROM exercises WHERE workoutType = :type")
    suspend fun getMaxSortOrder(type: String): Int?
}