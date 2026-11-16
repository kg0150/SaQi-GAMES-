package com.example.data

import com.example.localization.AppLanguage

enum class OfficerId {
    MOTU,
    PATLU
}

enum class DistrictId {
    PRECINCT,
    BAZAAR,
    PARK,
    DOWNTOWN
}

enum class TimeOfDayMode {
    DYNAMIC,
    DAY,
    SUNSET,
    NIGHT
}

enum class ScreenRoute {
    MENU,
    PLAYING,
    MISSIONS,
    CHARACTERS,
    SETTINGS
}

enum class ActionContextType {
    NONE,
    ARREST,
    TALK,
    INTERACT
}

data class MissionDef(
    val id: Int,
    val district: DistrictId,
    val rewardPoints: Int,
    val timeLimitSec: Float,
    val preferredTimeOfDay: Float? = null // 0..1 (0.25 = noon, 0.55 = sunset, 0.85 = night)
)

data class GameSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val dynamicShadows: Boolean = true,
    val timeOfDayMode: TimeOfDayMode = TimeOfDayMode.DYNAMIC
)

data class PlayerProgress(
    val selectedOfficer: OfficerId = OfficerId.MOTU,
    val totalScore: Int = 0,
    val highestUnlockedMission: Int = 1,
    val completedMissions: Set<Int> = emptySet(),
    val missionStars: Map<Int, Int> = emptyMap(),
    val lastMissionId: Int = 1
)

data class RadarBlip(
    val x: Float,
    val z: Float,
    val type: BlipType
)

enum class BlipType {
    PRECINCT,
    OBJECTIVE,
    CRIMINAL,
    POLICE_CAR,
    CIVILIAN
}

data class HudSnapshot(
    val activeMissionId: Int = 1, // 0 = Free Patrol
    val objectiveStep: Int = 0,
    val objectiveTextKey: String = "",
    val district: DistrictId = DistrictId.PRECINCT,
    val officer: OfficerId = OfficerId.MOTU,
    val health: Float = 100f,
    val stamina: Float = 100f,
    val scoreInSession: Int = 0,
    val timeRemainingSec: Float = 0f,
    val isInVehicle: Boolean = false,
    val canEnterVehicle: Boolean = false,
    val sirenOn: Boolean = false,
    val speedKmh: Int = 0,
    val actionContext: ActionContextType = ActionContextType.NONE,
    val playerX: Float = 0f,
    val playerZ: Float = 0f,
    val playerYawRad: Float = 0f,
    val radarBlips: List<RadarBlip> = emptyList(),
    val timeOfDayNormalized: Float = 0.28f,
    val dialogueText: String? = null,
    val isCinematicIntro: Boolean = false,
    val missionCompletePoints: Int? = null,
    val missionCompleteBonus: Int = 0,
    val missionCompleteStars: Int = 3,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false
)

object MissionCatalog {
    val missions: List<MissionDef> = listOf(
        MissionDef(id = 1, district = DistrictId.BAZAAR, rewardPoints = 500, timeLimitSec = 150f, preferredTimeOfDay = 0.25f),
        MissionDef(id = 2, district = DistrictId.PRECINCT, rewardPoints = 750, timeLimitSec = 160f, preferredTimeOfDay = 0.36f),
        MissionDef(id = 3, district = DistrictId.PARK, rewardPoints = 900, timeLimitSec = 170f, preferredTimeOfDay = 0.50f),
        MissionDef(id = 4, district = DistrictId.DOWNTOWN, rewardPoints = 1200, timeLimitSec = 180f, preferredTimeOfDay = 0.85f),
        MissionDef(id = 5, district = DistrictId.DOWNTOWN, rewardPoints = 2000, timeLimitSec = 240f, preferredTimeOfDay = 0.72f)
    )
}
