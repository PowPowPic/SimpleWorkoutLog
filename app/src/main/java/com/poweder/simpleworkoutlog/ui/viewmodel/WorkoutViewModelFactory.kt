package com.poweder.simpleworkoutlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.poweder.simpleworkoutlog.data.preferences.LastInputDataStore
import com.poweder.simpleworkoutlog.data.preferences.SettingsDataStore
import com.poweder.simpleworkoutlog.data.repository.WorkoutRepository

class WorkoutViewModelFactory(
    private val repository: WorkoutRepository,
    private val settingsDataStore: SettingsDataStore,
    private val lastInputDataStore: LastInputDataStore,
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            return WorkoutViewModel(repository, settingsDataStore, lastInputDataStore, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
