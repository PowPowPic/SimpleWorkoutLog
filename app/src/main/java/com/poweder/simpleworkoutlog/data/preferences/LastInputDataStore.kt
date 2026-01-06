package com.poweder.simpleworkoutlog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lastInputDataStore: DataStore<Preferences> by preferencesDataStore(name = "last_input")

/**
 * 種目ごとの前回入力値を保存するDataStore
 * - lastWeight: 前回の重量
 * - lastReps: 前回のレップ数
 */
class LastInputDataStore(private val context: Context) {
    
    // 種目IDごとにキーを生成
    private fun lastWeightKey(exerciseId: Long) = doublePreferencesKey("last_weight_$exerciseId")
    private fun lastRepsKey(exerciseId: Long) = intPreferencesKey("last_reps_$exerciseId")
    
    /**
     * 前回の重量を取得
     */
    fun getLastWeight(exerciseId: Long): Flow<Double> {
        return context.lastInputDataStore.data.map { preferences ->
            preferences[lastWeightKey(exerciseId)] ?: 0.0
        }
    }
    
    /**
     * 前回のレップ数を取得
     */
    fun getLastReps(exerciseId: Long): Flow<Int> {
        return context.lastInputDataStore.data.map { preferences ->
            preferences[lastRepsKey(exerciseId)] ?: 8 // デフォルト8reps
        }
    }
    
    /**
     * 前回値を保存
     */
    suspend fun saveLastInput(exerciseId: Long, weight: Double, reps: Int) {
        context.lastInputDataStore.edit { preferences ->
            preferences[lastWeightKey(exerciseId)] = weight
            preferences[lastRepsKey(exerciseId)] = reps
        }
    }
    
    /**
     * 特定種目の前回値をクリア
     */
    suspend fun clearLastInput(exerciseId: Long) {
        context.lastInputDataStore.edit { preferences ->
            preferences.remove(lastWeightKey(exerciseId))
            preferences.remove(lastRepsKey(exerciseId))
        }
    }
    
    /**
     * 全ての前回値をクリア
     */
    suspend fun clearAll() {
        context.lastInputDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
