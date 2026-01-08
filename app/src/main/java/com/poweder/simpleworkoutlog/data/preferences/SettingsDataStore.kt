package com.poweder.simpleworkoutlog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.poweder.simpleworkoutlog.util.DistanceUnit
import com.poweder.simpleworkoutlog.util.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val WEIGHT_UNIT_KEY = stringPreferencesKey("weight_unit")
        private val DISTANCE_UNIT_KEY = stringPreferencesKey("distance_unit")
        private val AD_REMOVED_KEY = booleanPreferencesKey("ad_removed")
        private val GRAPH_RESET_DATE_KEY = longPreferencesKey("graph_reset_logical_date")
    }

    // 言語設定
    val languageFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY]
    }

    suspend fun setLanguage(language: String?) {
        context.dataStore.edit { preferences ->
            if (language == null) {
                preferences.remove(LANGUAGE_KEY)
            } else {
                preferences[LANGUAGE_KEY] = language
            }
        }
    }

    // 重量単位設定
    val weightUnitFlow: Flow<WeightUnit> = context.dataStore.data.map { preferences ->
        val value = preferences[WEIGHT_UNIT_KEY] ?: WeightUnit.KG.name
        WeightUnit.valueOf(value)
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.dataStore.edit { preferences ->
            preferences[WEIGHT_UNIT_KEY] = unit.name
        }
    }

    // 距離単位設定
    val distanceUnitFlow: Flow<DistanceUnit> = context.dataStore.data.map { preferences ->
        val value = preferences[DISTANCE_UNIT_KEY] ?: DistanceUnit.KM.name
        DistanceUnit.valueOf(value)
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.dataStore.edit { preferences ->
            preferences[DISTANCE_UNIT_KEY] = unit.name
        }
    }

    // 広告削除フラグ
    val adRemovedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AD_REMOVED_KEY] ?: false
    }

    suspend fun setAdRemoved(removed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AD_REMOVED_KEY] = removed
        }
    }

    // グラフリセット起点日（EpochDay）- 上段カロリーグラフ用
    // nullの場合は全期間対象
    val graphResetDateFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[GRAPH_RESET_DATE_KEY]
    }

    suspend fun setGraphResetDate(epochDay: Long) {
        context.dataStore.edit { preferences ->
            preferences[GRAPH_RESET_DATE_KEY] = epochDay
        }
    }

    suspend fun clearGraphResetDate() {
        context.dataStore.edit { preferences ->
            preferences.remove(GRAPH_RESET_DATE_KEY)
        }
    }

    // ===== 下段グラフ用リセット（種目別/カテゴリ別） =====

    /**
     * 筋トレ種目別リセット日を取得
     */
    fun getStrengthGraphResetDate(exerciseId: Long): Flow<Long?> {
        val key = longPreferencesKey("graph_reset_strength_$exerciseId")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    /**
     * 筋トレ種目別リセット日を設定
     */
    suspend fun setStrengthGraphResetDate(exerciseId: Long, epochDay: Long) {
        val key = longPreferencesKey("graph_reset_strength_$exerciseId")
        context.dataStore.edit { preferences ->
            preferences[key] = epochDay
        }
    }

    /**
     * 有酸素種目別リセット日を取得
     */
    fun getCardioGraphResetDate(exerciseId: Long): Flow<Long?> {
        val key = longPreferencesKey("graph_reset_cardio_$exerciseId")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    /**
     * 有酸素種目別リセット日を設定
     */
    suspend fun setCardioGraphResetDate(exerciseId: Long, epochDay: Long) {
        val key = longPreferencesKey("graph_reset_cardio_$exerciseId")
        context.dataStore.edit { preferences ->
            preferences[key] = epochDay
        }
    }

    /**
     * スタジオカテゴリリセット日を取得
     */
    fun getStudioGraphResetDate(): Flow<Long?> {
        val key = longPreferencesKey("graph_reset_studio")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    /**
     * スタジオカテゴリリセット日を設定
     */
    suspend fun setStudioGraphResetDate(epochDay: Long) {
        val key = longPreferencesKey("graph_reset_studio")
        context.dataStore.edit { preferences ->
            preferences[key] = epochDay
        }
    }

    // ===== インタースティシャル広告の時間帯別表示記録 =====

    /**
     * 指定時間帯スロットの最終表示日を取得
     * @param slot 0:0-6時, 1:6-12時, 2:12-18時, 3:18-24時
     */
    fun getInterstitialLastShownDate(slot: Int): Flow<Long?> {
        val key = longPreferencesKey("interstitial_last_shown_slot_$slot")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    /**
     * 指定時間帯スロットの最終表示日を設定
     */
    suspend fun setInterstitialLastShownDate(slot: Int, epochDay: Long) {
        val key = longPreferencesKey("interstitial_last_shown_slot_$slot")
        context.dataStore.edit { preferences ->
            preferences[key] = epochDay
        }
    }
}