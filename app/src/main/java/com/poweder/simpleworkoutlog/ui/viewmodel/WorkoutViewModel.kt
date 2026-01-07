package com.poweder.simpleworkoutlog.ui.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poweder.simpleworkoutlog.data.entity.DailyWorkoutEntity
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutSessionEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutSetEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import com.poweder.simpleworkoutlog.data.model.SetItem
import com.poweder.simpleworkoutlog.data.preferences.LastInputDataStore
import com.poweder.simpleworkoutlog.data.preferences.SettingsDataStore
import com.poweder.simpleworkoutlog.data.repository.WorkoutRepository
import com.poweder.simpleworkoutlog.ui.screen.MonthlyStats
import com.poweder.simpleworkoutlog.util.DistanceUnit
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val settingsDataStore: SettingsDataStore,
    private val lastInputDataStore: LastInputDataStore,
    private val context: Context
) : ViewModel() {

    // ===== 設定 =====
    val weightUnit: StateFlow<WeightUnit> = settingsDataStore.weightUnitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)

    val distanceUnit: StateFlow<DistanceUnit> = settingsDataStore.distanceUnitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DistanceUnit.KM)

    val adRemoved: StateFlow<Boolean> = settingsDataStore.adRemovedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 言語設定
    val currentLanguage: StateFlow<String?> = settingsDataStore.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            settingsDataStore.setWeightUnit(unit)
        }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            settingsDataStore.setDistanceUnit(unit)
        }
    }

    /**
     * 言語を設定し、即座に反映
     */
    fun setLanguage(language: String?) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
            applyLanguage(language)
        }
    }

    /**
     * 言語を設定し、Activity再生成で確実に反映
     */
    fun setLanguageAndRecreate(language: String?, activity: android.app.Activity) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
            applyLanguage(language)
            activity.recreate()
        }
    }

    /**
     * 言語を即座に適用（AppCompatDelegate使用）
     */
    private fun applyLanguage(language: String?) {
        val localeList = if (language.isNullOrEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * 保存済み言語設定を適用（アプリ起動時に呼び出す）
     */
    fun applySavedLanguage() {
        viewModelScope.launch {
            val language = settingsDataStore.languageFlow.first()
            if (language != null) {
                applyLanguage(language)
            }
        }
    }

    // ===== 今日のサマリー（セッションから直接計算）=====

    // 今日のセッション
    private val todaySessions: StateFlow<List<WorkoutSessionEntity>> =
        repository.getSessionsByDate(currentLogicalDate().toEpochDay())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 今日のセット
    private val todaySets: StateFlow<List<WorkoutSetEntity>> =
        repository.getSetsForDate(currentLogicalDate().toEpochDay())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 今日の総挙上重量（セットから直接計算）
    val todayTotalWeight: StateFlow<Double> = todaySets.map { sets ->
        sets.sumOf { it.weight * it.reps }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 今日の運動時間（分）（セッションから直接計算）
    val todayTotalDuration: StateFlow<Int> = todaySessions.map { sessions ->
        sessions.sumOf { it.durationMinutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 今日の消費カロリー（セッションから直接計算）
    val todayTotalCalories: StateFlow<Int> = todaySessions.map { sessions ->
        sessions.sumOf { it.caloriesBurned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // レガシー：DailyWorkoutEntity（互換性のために維持）
    val todayWorkout: StateFlow<DailyWorkoutEntity?> = repository.getTodayWorkout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ===== 種目操作 =====
    val allExercises: StateFlow<List<ExerciseEntity>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 種目ID → 種目名のマップ
     */
    val exerciseNamesMap: StateFlow<Map<Long, String>> = repository.allExercises.map { exercises ->
        exercises.associate { it.id to (it.customName ?: it.name) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _strengthExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val strengthExercises: StateFlow<List<ExerciseEntity>> = _strengthExercises.asStateFlow()

    private val _cardioExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val cardioExercises: StateFlow<List<ExerciseEntity>> = _cardioExercises.asStateFlow()

    private val _intervalExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val intervalExercises: StateFlow<List<ExerciseEntity>> = _intervalExercises.asStateFlow()

    private val _studioExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val studioExercises: StateFlow<List<ExerciseEntity>> = _studioExercises.asStateFlow()

    private val _otherExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val otherExercises: StateFlow<List<ExerciseEntity>> = _otherExercises.asStateFlow()

    // ===== インターバル種目名（HIIT/Tabata） =====
    private val _intervalExerciseName = MutableStateFlow("HIIT")
    val intervalExerciseName: StateFlow<String> = _intervalExerciseName.asStateFlow()

    init {
        // 筋トレ種目を監視
        viewModelScope.launch {
            repository.getExercisesByType(WorkoutType.STRENGTH).collect { exercises ->
                _strengthExercises.value = exercises
            }
        }
        // 有酸素種目を監視
        viewModelScope.launch {
            repository.getExercisesByType(WorkoutType.CARDIO).collect { exercises ->
                _cardioExercises.value = exercises
            }
        }
        // インターバル種目を監視
        viewModelScope.launch {
            repository.getExercisesByType(WorkoutType.INTERVAL).collect { exercises ->
                _intervalExercises.value = exercises
            }
        }
        // スタジオ種目を監視
        viewModelScope.launch {
            repository.getExercisesByType(WorkoutType.STUDIO).collect { exercises ->
                _studioExercises.value = exercises
            }
        }
        // その他種目を監視
        viewModelScope.launch {
            repository.getExercisesByType(WorkoutType.OTHER).collect { exercises ->
                _otherExercises.value = exercises
            }
        }
    }

    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>> {
        return repository.getExercisesByType(type)
    }

    fun addExercise(name: String, type: String) {
        viewModelScope.launch {
            repository.insertExercise(name, type)
        }
    }

    fun updateExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch {
            repository.deleteExercise(id)
        }
    }

    // ===== Workout Type 管理 =====
    data class WorkoutTypeData(
        val id: String,
        val name: String,
        val nameResId: Int? = null,
        val isDefault: Boolean = false
    )

    private val _workoutTypes = MutableStateFlow<List<WorkoutTypeData>>(
        listOf(
            WorkoutTypeData(WorkoutType.STRENGTH, "Strength Training", com.poweder.simpleworkoutlog.R.string.workout_strength, true),
            WorkoutTypeData(WorkoutType.CARDIO, "Cardio", com.poweder.simpleworkoutlog.R.string.workout_cardio, true),
            WorkoutTypeData(WorkoutType.INTERVAL, "Interval Training", com.poweder.simpleworkoutlog.R.string.workout_interval, true)
        )
    )
    val workoutTypes: StateFlow<List<WorkoutTypeData>> = _workoutTypes.asStateFlow()

    fun addWorkoutType(name: String) {
        val newType = WorkoutTypeData(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            nameResId = null,
            isDefault = false
        )
        _workoutTypes.value = _workoutTypes.value + newType
    }

    fun updateWorkoutType(id: String, newName: String) {
        _workoutTypes.value = _workoutTypes.value.map { type ->
            if (type.id == id) {
                type.copy(name = newName, nameResId = null)
            } else {
                type
            }
        }
    }

    fun deleteWorkoutType(id: String) {
        _workoutTypes.value = _workoutTypes.value.filter { it.id != id }
    }

    // ===== 現在の種目 =====
    private val _currentExercise = MutableStateFlow<ExerciseEntity?>(null)
    val currentExercise: StateFlow<ExerciseEntity?> = _currentExercise.asStateFlow()

    fun setCurrentExercise(exercise: ExerciseEntity) {
        _currentExercise.value = exercise
    }

    fun setIntervalExerciseName(name: String) {
        _intervalExerciseName.value = name
    }

    // ===== セット管理（List<SetItem>方式） =====
    private val _setItems = MutableStateFlow<List<SetItem>>(emptyList())
    val setItems: StateFlow<List<SetItem>> = _setItems.asStateFlow()

    /**
     * 種目トータル（有効なセットの合計）
     */
    val sessionTotal: StateFlow<Double> = _setItems.map { items ->
        items.filter { it.isValid }.sumOf { it.totalWeight }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * セット入力画面を初期化（デフォルト3セット＋前回値）
     */
    fun initializeSetItems(exerciseId: Long) {
        viewModelScope.launch {
            val lastWeight = lastInputDataStore.getLastWeight(exerciseId).first()
            val lastReps = lastInputDataStore.getLastReps(exerciseId).first()

            val initialSets = (1..3).map { setNumber ->
                SetItem(
                    setNumber = setNumber,
                    weight = lastWeight,
                    reps = lastReps,
                    isConfirmed = false
                )
            }
            _setItems.value = initialSets
        }
    }

    /**
     * セットの値を更新
     */
    fun updateSetItem(itemId: String, weight: Double, reps: Int) {
        _setItems.value = _setItems.value.map { item ->
            if (item.id == itemId) {
                item.copy(weight = weight, reps = reps)
            } else {
                item
            }
        }
    }

    /**
     * セットを確定
     */
    fun confirmSetItem(itemId: String) {
        _setItems.value = _setItems.value.map { item ->
            if (item.id == itemId && item.isValid) {
                item.copy(isConfirmed = true)
            } else {
                item
            }
        }
    }

    /**
     * セットの確定を解除（編集可能に）
     */
    fun unconfirmSetItem(itemId: String) {
        _setItems.value = _setItems.value.map { item ->
            if (item.id == itemId) {
                item.copy(isConfirmed = false)
            } else {
                item
            }
        }
    }

    /**
     * セットを削除
     */
    fun deleteSetItem(itemId: String) {
        val currentItems = _setItems.value.toMutableList()
        currentItems.removeAll { it.id == itemId }

        _setItems.value = currentItems.mapIndexed { index, item ->
            item.copy(setNumber = index + 1)
        }
    }

    /**
     * 新しいセットを追加（前の行の値を引き継ぎ）
     */
    fun addNewSetItem() {
        val currentItems = _setItems.value
        val lastItem = currentItems.lastOrNull()

        val newSetNumber = currentItems.size + 1
        val newItem = SetItem(
            setNumber = newSetNumber,
            weight = lastItem?.weight ?: 0.0,
            reps = lastItem?.reps ?: 8,
            isConfirmed = false
        )

        _setItems.value = currentItems + newItem
    }

    /**
     * セッション完了＆保存（筋トレ用 - 運動時間と消費カロリー対応）
     * 有効なセット（weight > 0 && reps > 0）は確定状態に関係なく全て保存
     */
    fun finishAndSave(durationMinutes: Int = 0, caloriesBurned: Int = 0) {
        viewModelScope.launch {
            val exercise = _currentExercise.value ?: return@launch
            
            // 有効なセット（weight > 0 && reps > 0）は全て保存対象
            val validSets = _setItems.value.filter { it.isValid }

            if (validSets.isEmpty() && durationMinutes == 0 && caloriesBurned == 0) {
                clearSession()
                return@launch
            }

            val session = repository.getOrCreateSession(exercise.id, exercise.workoutType)

            // 有効なセットを全て保存
            validSets.forEach { setItem ->
                repository.addSet(session.id, setItem.weight, setItem.reps)
            }

            if (validSets.isNotEmpty()) {
                val lastSet = validSets.last()
                lastInputDataStore.saveLastInput(exercise.id, lastSet.weight, lastSet.reps)
            }

            // 運動時間と消費カロリーを保存
            val updatedSession = session.copy(
                durationMinutes = durationMinutes,
                caloriesBurned = caloriesBurned,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSession(updatedSession)

            clearSession()
        }
    }

    /**
     * セッションをクリア（保存せずにHomeへ戻る場合）
     */
    fun clearSession() {
        _currentExercise.value = null
        _setItems.value = emptyList()
    }

    /**
     * 未保存の有効なセットがあるか
     */
    fun hasUnsavedSets(): Boolean {
        return _setItems.value.any { it.isValid }
    }

    // ===== セッション操作（レガシー） =====
    private val _currentSession = MutableStateFlow<WorkoutSessionEntity?>(null)
    val currentSession: StateFlow<WorkoutSessionEntity?> = _currentSession.asStateFlow()

    private val _currentSets = MutableStateFlow<List<WorkoutSetEntity>>(emptyList())
    val currentSets: StateFlow<List<WorkoutSetEntity>> = _currentSets.asStateFlow()

    fun startSession(exerciseId: Long, workoutType: String) {
        viewModelScope.launch {
            val session = repository.getOrCreateSession(exerciseId, workoutType)
            _currentSession.value = session

            repository.getSetsBySession(session.id).collect { sets ->
                _currentSets.value = sets
            }
        }
    }

    fun addSet(weight: Double, reps: Int) {
        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch
            repository.addSet(session.id, weight, reps)
        }
    }

    fun updateSet(set: WorkoutSetEntity) {
        viewModelScope.launch {
            repository.updateSet(set)
        }
    }

    fun deleteSet(set: WorkoutSetEntity) {
        viewModelScope.launch {
            repository.deleteSet(set)
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch
            repository.updateSession(session)
            _currentSession.value = null
            _currentSets.value = emptyList()
        }
    }

    // ===== 全データ削除 =====
    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            lastInputDataStore.clearAll()
        }
    }

    // ===== 履歴・カレンダー・グラフ用 =====
    fun getDailyWorkoutsBetween(startDate: Long, endDate: Long): Flow<List<DailyWorkoutEntity>> {
        return repository.getDailyWorkoutsBetween(startDate, endDate)
    }

    /**
     * 指定日のセッションを取得
     */
    fun getSessionsForDate(date: LocalDate): Flow<List<WorkoutSessionEntity>> {
        return repository.getSessionsByDate(date.toEpochDay())
    }

    /**
     * 指定日のセッションを全削除
     */
    fun deleteSessionsByDate(date: LocalDate) {
        viewModelScope.launch {
            repository.deleteSessionsByDate(date.toEpochDay())
        }
    }

    /**
     * セッションを削除
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    /**
     * 指定月のワークアウト日（セッションがある日）を取得
     */
    fun getWorkoutDatesForMonth(yearMonth: YearMonth): Flow<Set<LocalDate>> {
        val startDate = yearMonth.atDay(1).toEpochDay()
        val endDate = yearMonth.atEndOfMonth().toEpochDay()

        return repository.getSessionDatesBetween(startDate, endDate).map { dates ->
            dates.map { LocalDate.ofEpochDay(it) }.toSet()
        }
    }

    /**
     * 指定月の統計を取得（セットから重量計算）
     */
    fun getMonthlyStats(yearMonth: YearMonth): Flow<MonthlyStats> {
        val startDate = yearMonth.atDay(1).toEpochDay()
        val endDate = yearMonth.atEndOfMonth().toEpochDay()

        return combine(
            repository.getSessionsBetween(startDate, endDate),
            repository.getSetsBetween(startDate, endDate)
        ) { sessions, sets ->
            if (sessions.isEmpty()) {
                MonthlyStats()
            } else {
                // ワークアウト日数（ユニークな日付数）
                val workoutDays = sessions.map { it.logicalDate }.distinct().size

                // 総運動時間
                val totalDuration = sessions.sumOf { it.durationMinutes }

                // 平均運動時間
                val averageDuration = if (workoutDays > 0) totalDuration / workoutDays else 0

                // 総消費カロリー
                val totalCalories = sessions.sumOf { it.caloriesBurned }

                // 総挙上重量（セットから計算）
                val totalWeight = sets.sumOf { it.weight * it.reps }

                MonthlyStats(
                    workoutDays = workoutDays,
                    totalDuration = totalDuration,
                    averageDuration = averageDuration,
                    totalCalories = totalCalories,
                    totalWeight = totalWeight
                )
            }
        }
    }

    /**
     * その他ワークアウトを保存
     */
    fun saveOtherWorkout(
        exerciseId: Long,
        durationMinutes: Int,
        caloriesBurned: Int
    ) {
        viewModelScope.launch {
            val session = repository.getOrCreateSession(exerciseId, WorkoutType.OTHER)

            val updatedSession = session.copy(
                durationMinutes = durationMinutes,
                caloriesBurned = caloriesBurned,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSession(updatedSession)
        }
    }

    /**
     * 有酸素ワークアウトを保存
     */
    fun saveCardioWorkout(
        exerciseId: Long,
        durationMinutes: Int,
        distance: Double,
        caloriesBurned: Int
    ) {
        viewModelScope.launch {
            val session = repository.getOrCreateSession(exerciseId, WorkoutType.CARDIO)

            val updatedSession = session.copy(
                durationMinutes = durationMinutes,
                distance = distance,
                caloriesBurned = caloriesBurned,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSession(updatedSession)
        }
    }

    /**
     * スタジオワークアウトを保存
     */
    fun saveStudioWorkout(
        exerciseId: Long,
        durationMinutes: Int,
        caloriesBurned: Int
    ) {
        viewModelScope.launch {
            val session = repository.getOrCreateSession(exerciseId, WorkoutType.STUDIO)

            val updatedSession = session.copy(
                durationMinutes = durationMinutes,
                caloriesBurned = caloriesBurned,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSession(updatedSession)
        }
    }

    /**
     * インターバルワークアウトを保存
     */
    fun saveIntervalWorkout(
        exerciseId: Long,
        durationSeconds: Int,
        sets: Int
    ) {
        viewModelScope.launch {
            val session = repository.getOrCreateSession(exerciseId, WorkoutType.INTERVAL)

            val durationMinutes = durationSeconds / 60
            val updatedSession = session.copy(
                durationMinutes = durationMinutes,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSession(updatedSession)
        }
    }

    /**
     * インターバルワークアウトを種目名で保存（HIIT/Tabata）- 消費カロリー対応
     */
    fun saveIntervalWorkoutByName(
        exerciseName: String,
        durationSeconds: Int,
        sets: Int,
        caloriesBurned: Int = 0
    ) {
        viewModelScope.launch {
            val durationMinutes = durationSeconds / 60

            // インターバル種目を検索または作成
            val exercises = repository.getExercisesByType(WorkoutType.INTERVAL).first()
            var exercise = exercises.find { it.name == exerciseName || it.customName == exerciseName }

            if (exercise == null) {
                val id = repository.insertExercise(exerciseName, WorkoutType.INTERVAL)
                exercise = ExerciseEntity(
                    id = id,
                    name = exerciseName,
                    workoutType = WorkoutType.INTERVAL,
                    sortOrder = 0
                )
            }

            val session = repository.getOrCreateSession(exercise.id, WorkoutType.INTERVAL)

            val updatedSession = session.copy(
                durationMinutes = durationMinutes,
                caloriesBurned = caloriesBurned,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSession(updatedSession)
        }
    }

    /**
     * 指定日のセットを取得
     */
    fun getSetsForDate(date: LocalDate): Flow<List<WorkoutSetEntity>> {
        return repository.getSetsForDate(date.toEpochDay())
    }
}
