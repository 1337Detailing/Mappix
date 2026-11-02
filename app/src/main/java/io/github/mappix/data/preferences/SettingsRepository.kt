package io.github.mappix.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mappixDataStore by preferencesDataStore(name = "mappix_settings")

data class AppSettings(
    val selectedProfileId: String? = null,
    val darkMode: Boolean = true,
    val overlay: Boolean = false,
    val onboardingSeen: Boolean = false,
    val language: String = "fr"
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val profileKey = stringPreferencesKey("selected_profile")
    private val darkKey = booleanPreferencesKey("dark_mode")
    private val overlayKey = booleanPreferencesKey("overlay")
    private val onboardingKey = booleanPreferencesKey("onboarding_seen")
    private val languageKey = stringPreferencesKey("language")

    val settings: Flow<AppSettings> = context.mappixDataStore.data.map {
        AppSettings(it[profileKey], it[darkKey] ?: true, it[overlayKey] ?: false,
            it[onboardingKey] ?: false, it[languageKey] ?: "fr")
    }

    suspend fun selectProfile(id: String?) = context.mappixDataStore.edit { if (id == null) it.remove(profileKey) else it[profileKey] = id }
    suspend fun setDarkMode(enabled: Boolean) = context.mappixDataStore.edit { it[darkKey] = enabled }
    suspend fun setOverlay(enabled: Boolean) = context.mappixDataStore.edit { it[overlayKey] = enabled }
    suspend fun setOnboardingSeen() = context.mappixDataStore.edit { it[onboardingKey] = true }
    suspend fun setLanguage(language: String) = context.mappixDataStore.edit { it[languageKey] = if (language == "en") "en" else "fr" }
}
