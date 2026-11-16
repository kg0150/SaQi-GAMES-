package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ScreenRoute
import com.example.game3d.PatrolGLSurfaceView
import com.example.localization.GameStrings
import com.example.ui.GameViewModel
import com.example.ui.components.TacticalHud
import com.example.ui.screens.CharacterInfoScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MissionResultOverlay
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.PauseOverlay
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PatrolCityTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()
    private var glSurfaceView: PatrolGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val progress by viewModel.progress.collectAsStateWithLifecycle()
            val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val hud by viewModel.hud.collectAsStateWithLifecycle()
            val areaUnlocked by viewModel.areaUnlockedBanner.collectAsStateWithLifecycle()
            val strings = remember(settings.language) { GameStrings.get(settings.language) }

            PatrolCityTheme(language = settings.language) {
                PatrolCityRoot(
                    viewModel = viewModel,
                    screen = screen,
                    settings = settings,
                    progress = progress,
                    hud = hud,
                    areaUnlocked = areaUnlocked,
                    strings = strings,
                    onGlViewCreated = { glSurfaceView = it }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
        viewModel.onActivityPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
        viewModel.onActivityResume()
    }
}

@Composable
private fun PatrolCityRoot(
    viewModel: GameViewModel,
    screen: ScreenRoute,
    settings: com.example.data.GameSettings,
    progress: com.example.data.PlayerProgress,
    hud: com.example.data.HudSnapshot,
    areaUnlocked: Boolean,
    strings: com.example.localization.LocalizedStrings,
    onGlViewCreated: (PatrolGLSurfaceView) -> Unit
) {
    var isRunningToggle by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Persistent Live 3D OpenGL ES 3.0 City Viewport
        AndroidView(
            factory = { ctx ->
                PatrolGLSurfaceView(ctx, viewModel.simulation).also { view ->
                    view.renderer.dynamicShadowsEnabled = settings.dynamicShadows
                    onGlViewCreated(view)
                }
            },
            update = { view ->
                view.renderer.dynamicShadowsEnabled = settings.dynamicShadows
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose {
                viewModel.simulation.input.joyX = 0f
                viewModel.simulation.input.joyY = 0f
            }
        }

        when (screen) {
            ScreenRoute.MENU -> {
                MainMenuScreen(
                    progress = progress,
                    currentLanguage = settings.language,
                    strings = strings,
                    onContinuePatrol = { viewModel.continuePatrol() },
                    onFreePatrol = { viewModel.startMission(0) },
                    onOpenMissions = { viewModel.navigateTo(ScreenRoute.MISSIONS) },
                    onOpenCharacters = { viewModel.navigateTo(ScreenRoute.CHARACTERS) },
                    onOpenSettings = { viewModel.navigateTo(ScreenRoute.SETTINGS) },
                    onSelectLanguage = { viewModel.setLanguage(it) }
                )
            }

            ScreenRoute.MISSIONS -> {
                MissionsScreen(
                    progress = progress,
                    strings = strings,
                    onStartMission = { id -> viewModel.startMission(id) },
                    onBack = {
                        val dest = if (viewModel.simulation.isPlaying) ScreenRoute.PLAYING else ScreenRoute.MENU
                        viewModel.navigateTo(dest)
                    }
                )
            }

            ScreenRoute.CHARACTERS -> {
                CharacterInfoScreen(
                    selectedOfficer = progress.selectedOfficer,
                    strings = strings,
                    onSelectOfficer = { viewModel.selectOfficer(it) },
                    onBack = { viewModel.navigateTo(ScreenRoute.MENU) }
                )
            }

            ScreenRoute.SETTINGS -> {
                SettingsScreen(
                    settings = settings,
                    strings = strings,
                    onSelectLanguage = { viewModel.setLanguage(it) },
                    onMusicToggle = { viewModel.setMusicEnabled(it) },
                    onSfxToggle = { viewModel.setSfxEnabled(it) },
                    onShadowsToggle = { viewModel.setDynamicShadows(it) },
                    onTimeModeSelect = { viewModel.setTimeMode(it) },
                    onBack = { viewModel.navigateTo(ScreenRoute.MENU) }
                )
            }

            ScreenRoute.PLAYING -> {
                BackHandler {
                    viewModel.togglePause()
                }

                TacticalHud(
                    hud = hud,
                    totalScore = progress.totalScore,
                    strings = strings,
                    isRunning = isRunningToggle,
                    onJoystickMoved = { jx, jy ->
                        viewModel.simulation.input.joyX = jx
                        viewModel.simulation.input.joyY = jy
                    },
                    onRunToggle = { running ->
                        isRunningToggle = running
                        viewModel.simulation.input.isRunning = running
                    },
                    onJump = { viewModel.simulation.triggerJump() },
                    onActionOrArrest = { viewModel.simulation.triggerActionOrArrest() },
                    onEnterExitVehicle = { viewModel.simulation.toggleEnterExitVehicle() },
                    onSirenToggle = { viewModel.simulation.toggleSiren() },
                    onGasChanged = { viewModel.simulation.input.gasPressed = it },
                    onBrakeChanged = { viewModel.simulation.input.brakePressed = it },
                    onSwitchOfficer = { viewModel.switchOfficerInGame() },
                    onOpenMissions = { viewModel.navigateTo(ScreenRoute.MISSIONS) },
                    onPause = { viewModel.togglePause() }
                )

                if (hud.isPaused && hud.missionCompletePoints == null && !hud.isGameOver) {
                    PauseOverlay(
                        strings = strings,
                        onResume = { viewModel.togglePause() },
                        onRestart = { viewModel.restartCurrentMission() },
                        onExitToMenu = { viewModel.exitGameplayToMenu() }
                    )
                }

                if (hud.missionCompletePoints != null || hud.isGameOver) {
                    MissionResultOverlay(
                        hud = hud,
                        areaUnlocked = areaUnlocked,
                        strings = strings,
                        onNextMission = { nextId -> viewModel.startMission(nextId) },
                        onRestart = { viewModel.restartCurrentMission() },
                        onExitToMenu = { viewModel.exitGameplayToMenu() }
                    )
                }
            }
        }
    }
}
