package com.poweder.simpleworkoutlog.data.dao

import androidx.room.*
import com.poweder.simpleworkoutlog.data.entity.DailyWorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyWorkoutDao {
    @Query("SELECT * FROM daily_workouts WHERE date = :date")
    fun getDailyWorkout(date: Long): Flow<DailyWorkoutEntity?>

    @Query("SELECT * FROM daily_workouts WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getDailyWorkoutsBetween(startDate: Long, endDate: Long): Flow<List<DailyWorkoutEntity>>

    @Query("SELECT * FROM daily_workouts ORDER BY date DESC")
    fun getAllDailyWorkouts(): Flow<List<DailyWorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyWorkout: DailyWorkoutEntity)

    @Update
    suspend fun update(dailyWorkout: DailyWorkoutEntity)

    @Query("DELETE FROM daily_workouts WHERE date = :date")
    suspend fun deleteByDate(date: Long)

    @Query("DELETE FROM daily_workouts")
    suspend fun deleteAll()

    @Query("SELECT SUM(totalWeight) FROM daily_workouts WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalWeightBetween(startDate: Long, endDate: Long): Flow<Double?>
}