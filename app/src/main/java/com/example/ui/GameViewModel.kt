package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.GamePreferencesRepository
import com.example.data.GameSettings
import com.example.data.HudSnapshot
import com.example.data.MissionCatalog
import com.example.data.OfficerId
import com.example.data.PlayerProgress
import com.example.data.ScreenRoute
import com.example.data.TimeOfDayMode
import com.example.game3d.CityWorld
import com.example.game3d.GameSimulation
import com.example.localization.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = GamePreferencesRepository(application)
    val soundEngine = SoundEngine(application)
    val world = CityWorld()
    val simulation = GameSimulation(world, soundEngine)

    val settings: StateFlow<GameSettings> = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        GameSettings()
    )

    val progress: StateFlow<PlayerProgress> = repo.progressFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PlayerProgress()
    )

    private val _currentScreen = MutableStateFlow(ScreenRoute.MENU)
    val currentScreen: StateFlow<ScreenRoute> = _currentScreen.asStateFlow()

    private val _hud = MutableStateFlow(HudSnapshot())
    val hud: StateFlow<HudSnapshot> = _hud.asStateFlow()

    private val _areaUnlockedBanner = MutableStateFlow(false)
    val areaUnlockedBanner: StateFlow<Boolean> = _areaUnlockedBanner.asStateFlow()

    init {
        simulation.onMissionCompletedCallback = { missionId, totalEarned, stars ->
            viewModelScope.launch {
                val prevHighest = progress.value.highestUnlockedMission
                repo.recordMissionCompletion(missionId, totalEarned, stars)
                if (missionId >= prevHighest && missionId < MissionCatalog.missions.size) {
                    _areaUnlockedBanner.value = true
                }
            }
        }

        viewModelScope.launch {
            settings.collectLatest { s ->
                soundEngine.updateSettings(s.musicEnabled, s.sfxEnabled)
                simulation.timeMode = s.timeOfDayMode
            }
        }

        viewModelScope.launch {
            progress.collectLatest { p ->
                if (!simulation.isPlaying) {
                    simulation.activeOfficer = p.selectedOfficer
                }
            }
        }

        // Poll simulation HUD snapshot cleanly at 25Hz during gameplay
        viewModelScope.launch {
            while (isActive) {
                if (_currentScreen.value == ScreenRoute.PLAYING) {
                    _hud.value = simulation.buildHudSnapshot()
                }
                delay(40L)
            }
        }
    }

    fun navigateTo(route: ScreenRoute) {
        soundEngine.playClick()
        _currentScreen.value = route
    }

    fun startMission(missionId: Int) {
        _areaUnlockedBanner.value = false
        simulation.activeOfficer = progress.value.selectedOfficer
        simulation.startMission(missionId)
        soundEngine.onEnterGameplay()
        _currentScreen.value = ScreenRoute.PLAYING
        _hud.value = simulation.buildHudSnapshot()
    }

    fun continuePatrol() {
        val nextId = progress.value.lastMissionId.coerceIn(1, MissionCatalog.missions.size)
        startMission(nextId)
    }

    fun togglePause() {
        soundEngine.playClick()
        simulation.isPaused = !simulation.isPaused
        _hud.value = simulation.buildHudSnapshot()
    }

    fun restartCurrentMission() {
        startMission(simulation.activeMissionId)
    }

    fun exitGameplayToMenu() {
        soundEngine.playClick()
        simulation.exitToMenu()
        soundEngine.onExitGameplay()
        _currentScreen.value = ScreenRoute.MENU
    }

    fun switchOfficerInGame() {
        simulation.switchOfficer()
        viewModelScope.launch {
            repo.setSelectedOfficer(simulation.activeOfficer)
        }
        _hud.value = simulation.buildHudSnapshot()
    }

    fun selectOfficer(officer: OfficerId) {
        soundEngine.playClick()
        simulation.activeOfficer = officer
        viewModelScope.launch {
            repo.setSelectedOfficer(officer)
        }
    }

    fun setLanguage(lang: AppLanguage) {
        soundEngine.playClick()
        viewModelScope.launch { repo.setLanguage(lang) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        soundEngine.playClick()
        viewModelScope.launch { repo.setMusicEnabled(enabled) }
    }

    fun setSfxEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setSfxEnabled(enabled) }
    }

    fun setDynamicShadows(enabled: Boolean) {
        soundEngine.playClick()
        viewModelScope.launch { repo.setDynamicShadows(enabled) }
    }

    fun setTimeMode(mode: TimeOfDayMode) {
        soundEngine.playClick()
        viewModelScope.launch { repo.setTimeMode(mode) }
    }

    fun onActivityPause() {
        soundEngine.onPause()
    }

    fun onActivityResume() {
        soundEngine.onResume()
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.release()
    }
}
