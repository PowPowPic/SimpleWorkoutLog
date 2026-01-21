package com.poweder.simpleworkoutlog.data.repository

import com.poweder.simpleworkoutlog.data.dao.DailyDistance
import com.poweder.simpleworkoutlog.data.dao.DailyMaxWeight
import com.poweder.simpleworkoutlog.data.dao.DailySessionCount
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
    
    /**
     * IDで種目を取得
     */
    suspend fun getExerciseById(id: Long): ExerciseEntity? {
        return exerciseDao.getExerciseById(id)
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
    
    /**
     * 種目の並び順を一括更新
     * リストのインデックスをsortOrderとして保存
     */
    suspend fun updateExerciseSortOrders(exercises: List<ExerciseEntity>) {
        exercises.forEachIndexed { index, exercise ->
            exerciseDao.updateSortOrder(exercise.id, index)
        }
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

    /**
     * セッションをIDで取得（編集用）
     */
    suspend fun getSessionById(sessionId: Long): WorkoutSessionEntity? {
        return workoutSessionDao.getSessionById(sessionId)
    }

    /**
     * セッションのセットを一括取得（編集用）
     */
    suspend fun getSetsBySessionSync(sessionId: Long): List<WorkoutSetEntity> {
        return workoutSetDao.getSetsBySessionSync(sessionId)
    }

    suspend fun getOrCreateSession(exerciseId: Long, workoutType: String): WorkoutSessionEntity {
        return getOrCreateSession(exerciseId, workoutType, currentLogicalDate().toEpochDay())
    }

    /**
     * 指定日付でセッションを取得または作成（過去のトレーニング追加用）
     */
    suspend fun getOrCreateSession(exerciseId: Long, workoutType: String, logicalDate: Long): WorkoutSessionEntity {
        val existing = workoutSessionDao.getSessionByExerciseAndDate(exerciseId, logicalDate)

        return existing ?: run {
            val newSession = WorkoutSessionEntity(
                exerciseId = exerciseId,
                logicalDate = logicalDate,
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

    /**
     * 指定期間のセッション日付リストを取得
     */
    fun getSessionDatesBetween(startDate: Long, endDate: Long): Flow<List<Long>> {
        return workoutSessionDao.getSessionDatesBetween(startDate, endDate)
    }

    /**
     * 指定期間のセッションを取得
     */
    fun getSessionsBetween(startDate: Long, endDate: Long): Flow<List<WorkoutSessionEntity>> {
        return workoutSessionDao.getSessionsBetween(startDate, endDate)
    }

    /**
     * 指定日のセッションを全削除
     */
    suspend fun deleteSessionsByDate(date: Long) {
        workoutSessionDao.deleteByDate(date)
    }

    /**
     * 指定日のセットを取得
     */
    fun getSetsForDate(date: Long): Flow<List<WorkoutSetEntity>> {
        return workoutSetDao.getSetsForDate(date)
    }

    /**
     * 指定期間のセットを取得
     */
    fun getSetsBetween(startDate: Long, endDate: Long): Flow<List<WorkoutSetEntity>> {
        return workoutSetDao.getSetsBetween(startDate, endDate)
    }

    /**
     * 最古のセッション日付を取得（グラフALL期間用）
     */
    fun getOldestSessionDate(): Flow<Long?> {
        return workoutSessionDao.getOldestSessionDate()
    }

    /**
     * 指定種目の日別MAX weightを取得（グラフ用）
     */
    fun getDailyMaxWeightForExercise(exerciseId: Long, startDate: Long): Flow<List<DailyMaxWeight>> {
        return workoutSetDao.getDailyMaxWeightForExercise(exerciseId, startDate)
    }

    /**
     * 指定Cardio種目の日別距離を取得（グラフ用）
     */
    fun getDailyDistanceForCardioExercise(exerciseId: Long, startDate: Long): Flow<List<DailyDistance>> {
        return workoutSessionDao.getDailyDistanceForCardioExercise(exerciseId, startDate)
    }

    /**
     * Studio全体の日別セッション数を取得（グラフ用）
     */
    fun getDailyStudioSessionCount(startDate: Long): Flow<List<DailySessionCount>> {
        return workoutSessionDao.getDailyStudioSessionCount(startDate)
    }
}
