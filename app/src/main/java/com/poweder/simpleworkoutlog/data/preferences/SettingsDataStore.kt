package com.poweder.simpleworkoutlog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
}
