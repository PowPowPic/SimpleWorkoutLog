package com.poweder.simpleworkoutlog.data.repository

import com.poweder.simpleworkoutlog.data.dao.DailyWorkoutDao
import com.poweder.simpleworkoutlog.data.dao.ExerciseDao
import com.poweder.simpleworkoutlog.data.dao.WorkoutSessionDao
import com.poweder.simpleworkoutlog.data.dao.WorkoutSetDao
import com.poweder.simpleworkoutlog.data.entity.DailyWorkoutEntity
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutSessionEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutSetEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val dailyWorkoutDao: DailyWorkoutDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutSetDao: WorkoutSetDao
) {
    // ===== Exercise（種目）操作 =====
    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>> {
        return exerciseDao.getExercisesByType(type)
    }

    suspend fun insertExercise(name: String, type: String): Long {
        val maxOrder = exerciseDao.getMaxSortOrder(type) ?: 0
        val exercise = ExerciseEntity(
            name = name,
            workoutType = type,
            sortOrder = maxOrder + 1
        )
        return exerciseDao.insert(exercise)
    }

    suspend fun updateExercise(exercise: ExerciseEntity) {
        exerciseDao.update(exercise)
    }

    suspend fun deleteExercise(id: Long) {
        exerciseDao.deleteById(id)
    }

    // ===== Daily Workout（日別サマリー）操作 =====
    fun getTodayWorkout(): Flow<DailyWorkoutEntity?> {
        return dailyWorkoutDao.getDailyWorkout(currentLogicalDate().toEpochDay())
    }

    fun getDailyWorkout(date: Long): Flow<DailyWorkoutEntity?> {
        return dailyWorkoutDao.getDailyWorkout(date)
    }

    fun getDailyWorkoutsBetween(startDate: Long, endDate: Long): Flow<List<DailyWorkoutEntity>> {
        return dailyWorkoutDao.getDailyWorkoutsBetween(startDate, endDate)
    }

    suspend fun updateDailyWorkout(dailyWorkout: DailyWorkoutEntity) {
        dailyWorkoutDao.insert(dailyWorkout)
    }

    // ===== Workout Session（セッション）操作 =====
    fun getSessionsByDate(date: Long): Flow<List<WorkoutSessionEntity>> {
        return workoutSessionDao.getSessionsByDate(date)
    }

    suspend fun getOrCreateSession(exerciseId: Long, workoutType: String): WorkoutSessionEntity {
        val date = currentLogicalDate().toEpochDay()
        val existing = workoutSessionDao.getSessionByExerciseAndDate(exerciseId, date)

        return existing ?: run {
            val newSession = WorkoutSessionEntity(
                exerciseId = exerciseId,
                logicalDate = date,
                workoutType = workoutType
            )
            val id = workoutSessionDao.insert(newSession)
            newSession.copy(id = id)
        }
    }

    suspend fun updateSession(session: WorkoutSessionEntity) {
        workoutSessionDao.update(session.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSession(id: Long) {
        workoutSessionDao.deleteById(id)
    }

    // ===== Workout Set（セット）操作 =====
    fun getSetsBySession(sessionId: Long): Flow<List<WorkoutSetEntity>> {
        return workoutSetDao.getSetsBySession(sessionId)
    }

    suspend fun addSet(sessionId: Long, weight: Double, reps: Int): Long {
        val maxSetNumber = workoutSetDao.getMaxSetNumber(sessionId) ?: 0
        val totalWeight = weight * reps

        val set = WorkoutSetEntity(
            sessionId = sessionId,
            setNumber = maxSetNumber + 1,
            weight = weight,
            reps = reps,
            totalWeight = totalWeight
        )

        val setId = workoutSetDao.insert(set)

        // セッションの総重量を更新
        updateSessionTotalWeight(sessionId)

        return setId
    }

    suspend fun updateSet(set: WorkoutSetEntity) {
        val updatedSet = set.copy(totalWeight = set.weight * set.reps)
        workoutSetDao.update(updatedSet)

        // セッションの総重量を更新
        updateSessionTotalWeight(set.sessionId)
    }

    suspend fun deleteSet(set: WorkoutSetEntity) {
        workoutSetDao.delete(set)

        // セッションの総重量を更新
        updateSessionTotalWeight(set.sessionId)
    }

    private suspend fun updateSessionTotalWeight(sessionId: Long) {
        val session = workoutSessionDao.getSessionById(sessionId) ?: return
        val totalWeight = workoutSetDao.getTotalWeightBySession(sessionId) ?: 0.0

        workoutSessionDao.update(session.copy(
            totalWeight = totalWeight,
            updatedAt = System.currentTimeMillis()
        ))

        // 日別サマリーも更新
        updateDailyTotalWeight(session.logicalDate)
    }

    private suspend fun updateDailyTotalWeight(date: Long) {
        // 今日のすべてのセッションから総重量を計算
        val dailyWorkout = DailyWorkoutEntity(
            date = date,
            totalWeight = 0.0, // TODO: 全セッションから計算
            updatedAt = System.currentTimeMillis()
        )
        dailyWorkoutDao.insert(dailyWorkout)
    }

    // ===== 全データ削除 =====
    suspend fun deleteAllData() {
        workoutSetDao.deleteAll()
        workoutSessionDao.deleteAll()
        dailyWorkoutDao.deleteAll()
        exerciseDao.deleteAll()
    }
}