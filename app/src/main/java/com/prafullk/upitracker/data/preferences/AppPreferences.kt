package com.prafullk.upitracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "paylens_prefs")

class AppPreferences(private val context: Context) {

    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    private val MONTHLY_BUDGET_KEY = doublePreferencesKey("monthly_budget")

    val isOnboardingCompleted: Flow<Boolean> =
            context.dataStore.data.map { preferences ->
                preferences[ONBOARDING_COMPLETED_KEY] ?: false
            }

    val monthlyBudget: Flow<Double> =
            context.dataStore.data.map { prefs -> prefs[MONTHLY_BUDGET_KEY] ?: 15000.0 }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences -> preferences[ONBOARDING_COMPLETED_KEY] = completed }
    }

    suspend fun saveMonthlyBudget(budget: Double) {
        context.dataStore.edit { prefs -> prefs[MONTHLY_BUDGET_KEY] = budget }
    }
}
