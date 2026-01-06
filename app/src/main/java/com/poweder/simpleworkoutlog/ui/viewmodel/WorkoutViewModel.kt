package com.poweder.simpleworkoutlog.ui.viewmodel

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
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
import com.poweder.simpleworkoutlog.util.DistanceUnit
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
            // Activity再生成で確実に反映
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
    
    // ===== 今日のサマリー =====
    val todayWorkout: StateFlow<DailyWorkoutEntity?> = repository.getTodayWorkout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val todayTotalWeight: StateFlow<Double> = repository.getTodayWorkout()
        .map { it?.totalWeight ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    // ===== 種目操作 =====
    val allExercises: StateFlow<List<ExerciseEntity>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _strengthExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val strengthExercises: StateFlow<List<ExerciseEntity>> = _strengthExercises.asStateFlow()
    
    private val _cardioExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val cardioExercises: StateFlow<List<ExerciseEntity>> = _cardioExercises.asStateFlow()
    
    private val _intervalExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val intervalExercises: StateFlow<List<ExerciseEntity>> = _intervalExercises.asStateFlow()
    
    private val _studioExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val studioExercises: StateFlow<List<ExerciseEntity>> = _studioExercises.asStateFlow()
    
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
    
    // ===== セット管理（List<SetItem>方式） =====
    private val _setItems = MutableStateFlow<List<SetItem>>(emptyList())
    val setItems: StateFlow<List<SetItem>> = _setItems.asStateFlow()
    
    /**
     * 種目トータル（確定済みセットの合計）
     */
    val sessionTotal: StateFlow<Double> = _setItems.map { items ->
        items.filter { it.isConfirmed }.sumOf { it.totalWeight }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    /**
     * セット入力画面を初期化（デフォルト3セット＋前回値）
     */
    fun initializeSetItems(exerciseId: Long) {
        viewModelScope.launch {
            val lastWeight = lastInputDataStore.getLastWeight(exerciseId).first()
            val lastReps = lastInputDataStore.getLastReps(exerciseId).first()
            
            // デフォルト3セットを前回値で初期化
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
        
        // セット番号を振り直し
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
     * セッション完了＆保存
     */
    fun finishAndSave() {
        viewModelScope.launch {
            val exercise = _currentExercise.value ?: return@launch
            val confirmedSets = _setItems.value.filter { it.isConfirmed && it.isValid }
            
            if (confirmedSets.isEmpty()) {
                // 確定済みセットがなければ何もしない
                clearSession()
                return@launch
            }
            
            // セッションを作成/取得
            val session = repository.getOrCreateSession(exercise.id, exercise.workoutType)
            
            // 各セットをDBに保存
            confirmedSets.forEach { setItem ->
                repository.addSet(session.id, setItem.weight, setItem.reps)
            }
            
            // 前回値を保存（最後のセットの値）
            val lastSet = confirmedSets.last()
            lastInputDataStore.saveLastInput(exercise.id, lastSet.weight, lastSet.reps)
            
            // セッション完了
            repository.updateSession(session)
            
            // 状態クリア
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
     * 未保存のセットがあるか
     */
    fun hasUnsavedSets(): Boolean {
        return _setItems.value.any { it.isConfirmed && it.isValid }
    }
    
    // ===== セッション操作（レガシー - 後方互換性のため残す） =====
    private val _currentSession = MutableStateFlow<WorkoutSessionEntity?>(null)
    val currentSession: StateFlow<WorkoutSessionEntity?> = _currentSession.asStateFlow()
    
    private val _currentSets = MutableStateFlow<List<WorkoutSetEntity>>(emptyList())
    val currentSets: StateFlow<List<WorkoutSetEntity>> = _currentSets.asStateFlow()
    
    fun startSession(exerciseId: Long, workoutType: String) {
        viewModelScope.launch {
            val session = repository.getOrCreateSession(exerciseId, workoutType)
            _currentSession.value = session
            
            // セットを監視
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
}
