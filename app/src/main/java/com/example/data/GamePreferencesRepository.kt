package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.patrolDataStore: DataStore<Preferences> by preferencesDataStore(name = "patrol_city_prefs")

class GamePreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
        val DYNAMIC_SHADOWS = booleanPreferencesKey("dynamic_shadows")
        val TIME_MODE = stringPreferencesKey("time_mode")

        val SELECTED_OFFICER = stringPreferencesKey("selected_officer")
        val TOTAL_SCORE = intPreferencesKey("total_score")
        val HIGHEST_UNLOCKED_MISSION = intPreferencesKey("highest_unlocked_mission")
        val COMPLETED_MISSIONS = stringPreferencesKey("completed_missions")
        val MISSION_STARS = stringPreferencesKey("mission_stars")
        val LAST_MISSION_ID = intPreferencesKey("last_mission_id")
    }

    val settingsFlow: Flow<GameSettings> = context.patrolDataStore.data.map { prefs ->
        GameSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
            sfxEnabled = prefs[Keys.SFX_ENABLED] ?: true,
            dynamicShadows = prefs[Keys.DYNAMIC_SHADOWS] ?: true,
            timeOfDayMode = prefs[Keys.TIME_MODE]?.let {
                runCatching { TimeOfDayMode.valueOf(it) }.getOrNull()
            } ?: TimeOfDayMode.DYNAMIC
        )
    }

    val progressFlow: Flow<PlayerProgress> = context.patrolDataStore.data.map { prefs ->
        val completed = prefs[Keys.COMPLETED_MISSIONS]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

        val stars = prefs[Keys.MISSION_STARS]
            ?.split(",")
            ?.mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val m = parts[0].toIntOrNull()
                    val s = parts[1].toIntOrNull()
                    if (m != null && s != null) m to s else null
                } else null
            }?.toMap() ?: emptyMap()

        val officer = prefs[Keys.SELECTED_OFFICER]?.let {
            runCatching { OfficerId.valueOf(it) }.getOrNull()
        } ?: OfficerId.MOTU

        PlayerProgress(
            selectedOfficer = officer,
            totalScore = prefs[Keys.TOTAL_SCORE] ?: 0,
            highestUnlockedMission = (prefs[Keys.HIGHEST_UNLOCKED_MISSION] ?: 1).coerceIn(1, 5),
            completedMissions = completed,
            missionStars = stars,
            lastMissionId = prefs[Keys.LAST_MISSION_ID] ?: 1
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.patrolDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.patrolDataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
    }

    suspend fun setSfxEnabled(enabled: Boolean) {
        context.patrolDataStore.edit { it[Keys.SFX_ENABLED] = enabled }
    }

    suspend fun setDynamicShadows(enabled: Boolean) {
        context.patrolDataStore.edit { it[Keys.DYNAMIC_SHADOWS] = enabled }
    }

    suspend fun setTimeMode(mode: TimeOfDayMode) {
        context.patrolDataStore.edit { it[Keys.TIME_MODE] = mode.name }
    }

    suspend fun setSelectedOfficer(officer: OfficerId) {
        context.patrolDataStore.edit { it[Keys.SELECTED_OFFICER] = officer.name }
    }

    suspend fun recordMissionCompletion(missionId: Int, earnedPoints: Int, stars: Int) {
        context.patrolDataStore.edit { prefs ->
            val prevScore = prefs[Keys.TOTAL_SCORE] ?: 0
            prefs[Keys.TOTAL_SCORE] = prevScore + earnedPoints

            val prevUnlocked = prefs[Keys.HIGHEST_UNLOCKED_MISSION] ?: 1
            if (missionId >= prevUnlocked && missionId < MissionCatalog.missions.size) {
                prefs[Keys.HIGHEST_UNLOCKED_MISSION] = missionId + 1
            }

            val completed = (prefs[Keys.COMPLETED_MISSIONS]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toMutableSet() ?: mutableSetOf()).apply { add(missionId) }
            prefs[Keys.COMPLETED_MISSIONS] = completed.sorted().joinToString(",")

            val starMap = (prefs[Keys.MISSION_STARS]
                ?.split(",")
                ?.mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val m = parts[0].toIntOrNull()
                        val s = parts[1].toIntOrNull()
                        if (m != null && s != null) m to s else null
                    } else null
                }?.toMap()?.toMutableMap() ?: mutableMapOf())
            val oldStars = starMap[missionId] ?: 0
            if (stars > oldStars) starMap[missionId] = stars
            prefs[Keys.MISSION_STARS] = starMap.entries.joinToString(",") { "${it.key}:${it.value}" }
            prefs[Keys.LAST_MISSION_ID] = (missionId + 1).coerceAtMost(MissionCatalog.missions.size)
        }
    }
}
