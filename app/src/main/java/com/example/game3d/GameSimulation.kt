package com.example.game3d

import com.example.audio.SoundEngine
import com.example.data.ActionContextType
import com.example.data.BlipType
import com.example.data.HudSnapshot
import com.example.data.MissionCatalog
import com.example.data.OfficerId
import com.example.data.RadarBlip
import com.example.data.TimeOfDayMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NpcPerson(
    val id: Int,
    val role: CharacterRole,
    val pos: Vec3,
    var yawRad: Float,
    val pose: CharacterPose = CharacterPose(),
    var loopIndex: Int = 0,
    var waypointIndex: Int = 0,
    var isActiveTarget: Boolean = false,
    var isApprehended: Boolean = false,
    var isProtectedChecked: Boolean = false,
    var fleeSpeed: Float = 4.6f
)

data class PlayerInputState(
    @Volatile var joyX: Float = 0f,
    @Volatile var joyY: Float = 0f,
    @Volatile var isRunning: Boolean = false,
    @Volatile var gasPressed: Boolean = false,
    @Volatile var brakePressed: Boolean = false,
    @Volatile var camOrbitDeltaYaw: Float = 0f,
    @Volatile var camOrbitDeltaPitch: Float = 0f
)

class GameSimulation(
    val world: CityWorld,
    private val soundEngine: SoundEngine
) {
    val input = PlayerInputState()

    // Player & Partner state
    var activeOfficer = OfficerId.MOTU
    val playerPos = Vec3(-22f, 0f, -15f)
    var playerYaw = 0f
    var playerVelY = 0f
    var isGrounded = true
    val playerPose = CharacterPose()

    val partnerPos = Vec3(-25f, 0f, -16f)
    var partnerYaw = 0.4f
    val partnerPose = CharacterPose()

    var health = 100f
    var stamina = 100f
    var sessionScore = 0

    // Vehicle state
    val vehicles = ArrayList<VehicleEntity>()
    var drivenVehicleId: Int? = null
    var getawayVehicleId: Int? = null

    // NPCs
    val civilians = ArrayList<NpcPerson>()
    val criminals = ArrayList<NpcPerson>()

    // Camera state
    val camPos = Vec3(-22f, 4.5f, -23f)
    val camTarget = Vec3(-22f, 1.5f, -15f)
    var camYawOffset = 0f
    var camPitch = 0.36f
    var camFov = 58f
    private var cinematicTimer = 0f
    private var arrestCamTimer = 0f

    // Mission & World Time
    var isPlaying = false
    var isPaused = false
    var activeMissionId = 1
    var objectiveStep = 0
    var missionTimeRemaining = 150f
    var checkpointPos: Vec3? = null
    var checkpointVisible = false
    var dialogueKey: String? = null
    var dialogueTimer = 0f
    var missionCompletePoints: Int? = null
    var missionCompleteBonus = 0
    var missionCompleteStars = 3
    var isGameOver = false

    var timeMode = TimeOfDayMode.DYNAMIC
    var timeOfDayNormalized = 0.28f // 0..1 (0.25 noon, 0.52 sunset, 0.85 night)
    var worldTimeSec = 0f
    private var footstepCooldown = 0f
    private var actionAnimCooldown = 0f
    private var menuOrbitAngle = 0f

    var onMissionCompletedCallback: ((missionId: Int, totalEarned: Int, stars: Int) -> Unit)? = null

    init {
        resetWorldEntities()
    }

    fun startMission(missionId: Int) {
        activeMissionId = missionId
        isPlaying = true
        isPaused = false
        isGameOver = false
        missionCompletePoints = null
        objectiveStep = 0
        sessionScore = 0
        health = 100f
        stamina = 100f
        drivenVehicleId = null
        getawayVehicleId = null
        camYawOffset = 0f
        camPitch = 0.34f

        resetWorldEntities()

        val def = MissionCatalog.missions.firstOrNull { it.id == missionId }
        missionTimeRemaining = def?.timeLimitSec ?: 0f
        if (timeMode == TimeOfDayMode.DYNAMIC && def?.preferredTimeOfDay != null) {
            timeOfDayNormalized = def.preferredTimeOfDay
        }

        // Configure mission-specific objectives & criminals
        when (missionId) {
            1 -> {
                playerPos.set(-22f, 0f, -16f)
                playerYaw = 1.2f
                checkpointPos = Vec3(26f, 0f, -18f)
                checkpointVisible = true
                spawnCriminal(1, Vec3(32f, 0f, -30f), fleeSpeed = 4.4f, active = false)
                // Protected civilian in Bazaar
                civilians.add(NpcPerson(901, CharacterRole.CIVILIAN, Vec3(25f, 0f, -16f), -1.5f, isActiveTarget = true))
                triggerDialogue(if (activeOfficer == OfficerId.MOTU) "dialogue_motu" else "dialogue_patlu", 4.0f)
            }
            2 -> {
                playerPos.set(-18f, 0f, -18f)
                playerYaw = 0f
                checkpointPos = Vec3(-14f, 0f, -16f) // Near Police Cruiser
                checkpointVisible = true
                val getaway = VehicleEntity(
                    id = 99,
                    type = VehicleType.GETAWAY_CAR,
                    pos = Vec3(0f, 0f, -55f),
                    yawRad = 1.57f,
                    speed = 11.5f,
                    occupiedRole = CharacterRole.CRIMINAL,
                    waypointIndex = 1
                )
                vehicles.add(getaway)
                getawayVehicleId = 99
                triggerDialogue(if (activeOfficer == OfficerId.MOTU) "dialogue_motu" else "dialogue_patlu", 4.0f)
            }
            3 -> {
                playerPos.set(-24f, 0f, -12f)
                playerYaw = 0f
                checkpointPos = Vec3(-30f, 0f, 22f) // Emerald Park plaza
                checkpointVisible = true
                spawnCriminal(1, Vec3(-22f, 0f, 34f), fleeSpeed = 4.6f, active = false)
                spawnCriminal(2, Vec3(-38f, 0f, 32f), fleeSpeed = 4.8f, active = false)
                triggerDialogue(if (activeOfficer == OfficerId.MOTU) "dialogue_motu" else "dialogue_patlu", 4.0f)
            }
            4 -> {
                playerPos.set(-22f, 0f, -16f)
                playerYaw = 0.8f
                checkpointPos = Vec3(30f, 0f, 28f) // Downtown Plaza
                checkpointVisible = true
                spawnCriminal(1, Vec3(34f, 0f, 36f), fleeSpeed = 5.3f, active = false)
                triggerDialogue(if (activeOfficer == OfficerId.MOTU) "dialogue_motu" else "dialogue_patlu", 4.0f)
            }
            5 -> {
                playerPos.set(-22f, 0f, -16f)
                playerYaw = 0.5f
                checkpointPos = Vec3(26f, 0f, -20f) // Step 1: Bazaar, Step 2: Park, Step 3: Boss in Downtown
                checkpointVisible = true
                spawnCriminal(1, Vec3(30f, 0f, 32f), fleeSpeed = 5.5f, active = false)
                triggerDialogue(if (activeOfficer == OfficerId.MOTU) "dialogue_motu" else "dialogue_patlu", 4.5f)
            }
            else -> {
                // Free Patrol
                playerPos.set(-22f, 0f, -16f)
                playerYaw = 0f
                checkpointPos = null
                checkpointVisible = false
                spawnCriminal(1, Vec3(34f, 0f, -24f), fleeSpeed = 4.2f, active = true)
            }
        }

        cinematicTimer = if (missionId > 0) 2.2f else 0f
        soundEngine.playMissionStart()
    }

    fun exitToMenu() {
        isPlaying = false
        isPaused = false
        drivenVehicleId = null
        soundEngine.updateVehicleAudio(false, 0f, false)
        resetWorldEntities()
    }

    fun switchOfficer() {
        activeOfficer = if (activeOfficer == OfficerId.MOTU) OfficerId.PATLU else OfficerId.MOTU
        // Swap positions smoothly if on foot
        if (drivenVehicleId == null) {
            val tmpX = playerPos.x; val tmpZ = playerPos.z; val tmpYaw = playerYaw
            playerPos.x = partnerPos.x; playerPos.z = partnerPos.z; playerYaw = partnerYaw
            partnerPos.x = tmpX; partnerPos.z = tmpZ; partnerYaw = tmpYaw
        } else {
            val v = vehicles.firstOrNull { it.id == drivenVehicleId }
            v?.occupiedRole = activeRole()
        }
        soundEngine.playClick()
    }

    fun triggerJump() {
        if (!isPlaying || isPaused || drivenVehicleId != null || cinematicTimer > 0f) return
        if (isGrounded) {
            playerVelY = 7.4f
            isGrounded = false
            playerPose.animState = CharacterAnimState.JUMP
            soundEngine.playJump()
        }
    }

    fun toggleEnterExitVehicle() {
        if (!isPlaying || isPaused || cinematicTimer > 0f) return
        val currentId = drivenVehicleId
        if (currentId != null) {
            // Exit vehicle safely to driver's left side
            val v = vehicles.firstOrNull { it.id == currentId }
            if (v != null) {
                v.occupiedByPlayer = false
                v.occupiedRole = null
                v.sirenOn = false
                v.speed *= 0.25f
                val sideX = -cosF(v.yawRad) * 2.1f
                val sideZ = sinF(v.yawRad) * 2.1f
                playerPos.set(v.pos.x + sideX, 0f, v.pos.z + sideZ)
                Math3D.pushOutCircleVsBoxes(playerPos, 0.55f, world.colliders)
                playerYaw = v.yawRad
            }
            drivenVehicleId = null
            soundEngine.playVehicleDoor()
            soundEngine.updateVehicleAudio(false, 0f, false)

            // Mission 2 progression: if exited near stopped getaway car, move to arrest step
            if (activeMissionId == 2 && objectiveStep == 1) {
                val gv = vehicles.firstOrNull { it.id == getawayVehicleId }
                if (gv != null && gv.speed < 2f && playerPos.distXZ(gv.pos.x, gv.pos.z) < 12f) {
                    objectiveStep = 2
                    checkpointVisible = false
                    spawnCriminal(1, Vec3(gv.pos.x + 2.2f, 0f, gv.pos.z), fleeSpeed = 3.2f, active = true)
                }
            }
        } else {
            val nearest = findNearestEnterableVehicle()
            if (nearest != null) {
                drivenVehicleId = nearest.id
                nearest.occupiedByPlayer = true
                nearest.occupiedRole = activeRole()
                soundEngine.playVehicleDoor()
                if (activeMissionId == 2 && objectiveStep == 0) {
                    objectiveStep = 1
                    checkpointVisible = false
                    nearest.sirenOn = true
                }
            }
        }
    }

    fun toggleSiren() {
        val currentId = drivenVehicleId ?: return
        val v = vehicles.firstOrNull { it.id == currentId } ?: return
        if (v.type == VehicleType.POLICE_CRUISER || v.type == VehicleType.POLICE_SUV) {
            v.sirenOn = !v.sirenOn
            soundEngine.playClick()
        }
    }

    fun triggerActionOrArrest() {
        if (!isPlaying || isPaused || drivenVehicleId != null || cinematicTimer > 0f) return
        actionAnimCooldown = 0.45f
        playerPose.animState = CharacterAnimState.POLICE_ACTION
        playerPose.actionTimer = 0f

        val arrestRange = if (activeOfficer == OfficerId.MOTU) 3.8f else 3.2f
        val targetCrim = criminals.firstOrNull {
            !it.isApprehended && it.isActiveTarget && playerPos.distXZ(it.pos.x, it.pos.z) <= arrestRange
        }
        if (targetCrim != null) {
            targetCrim.isApprehended = true
            targetCrim.isActiveTarget = false
            sessionScore += 250
            arrestCamTimer = 1.1f
            soundEngine.playArrest()
            triggerDialogue("dialogue_arrest", 2.8f)
            onCriminalArrested()
            return
        }

        // Check civilian interaction
        val targetCiv = civilians.firstOrNull {
            playerPos.distXZ(it.pos.x, it.pos.z) <= 3.6f && (it.isActiveTarget || !it.isProtectedChecked)
        }
        if (targetCiv != null) {
            targetCiv.isProtectedChecked = true
            targetCiv.pose.isTalking = true
            targetCiv.pose.animState = CharacterAnimState.TALKING
            sessionScore += 100
            soundEngine.playClick()
            triggerDialogue("dialogue_civ", 3.0f)

            if (activeMissionId == 1 && objectiveStep == 2) {
                completeMission()
            }
        } else {
            soundEngine.playClick()
        }
    }

    private fun onCriminalArrested() {
        when (activeMissionId) {
            1 -> {
                if (objectiveStep == 1) {
                    objectiveStep = 2
                    val civ = civilians.firstOrNull { it.id == 901 }
                    checkpointPos = civ?.pos?.let { Vec3(it.x, 0f, it.z) } ?: Vec3(25f, 0f, -16f)
                    checkpointVisible = true
                }
            }
            2 -> {
                if (objectiveStep == 2) {
                    completeMission()
                }
            }
            3 -> {
                val remaining = criminals.count { !it.isApprehended }
                if (remaining == 0) {
                    objectiveStep = 2
                    checkpointPos = Vec3(-22f, 0f, -15f)
                    checkpointVisible = true
                }
            }
            4 -> {
                if (objectiveStep == 1) {
                    objectiveStep = 2
                    checkpointPos = Vec3(-22f, 0f, -15f)
                    checkpointVisible = true
                }
            }
            5 -> {
                if (objectiveStep == 2) {
                    completeMission()
                }
            }
            0 -> {
                // Respawn a new street suspect in Free Patrol after arrest!
                val spots = arrayOf(Vec3(34f, 0f, -26f), Vec3(-34f, 0f, 34f), Vec3(36f, 0f, 32f))
                val pick = spots[sessionScore % spots.size]
                spawnCriminal(criminals.size + 1, pick, fleeSpeed = 4.4f, active = true)
            }
        }
    }

    private fun completeMission() {
        if (missionCompletePoints != null) return
        val def = MissionCatalog.missions.firstOrNull { it.id == activeMissionId } ?: return
        val timeBonus = (missionTimeRemaining * 4f).toInt().coerceAtLeast(0)
        val total = def.rewardPoints + sessionScore + timeBonus
        val stars = when {
            missionTimeRemaining > def.timeLimitSec * 0.45f -> 3
            missionTimeRemaining > def.timeLimitSec * 0.18f -> 2
            else -> 1
        }
        missionCompletePoints = total
        missionCompleteBonus = timeBonus
        missionCompleteStars = stars
        soundEngine.updateVehicleAudio(false, 0f, false)
        soundEngine.playMissionComplete()
        onMissionCompletedCallback?.invoke(activeMissionId, total, stars)
    }

    fun step(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        worldTimeSec += dt

        // Update time of day
        timeOfDayNormalized = when (timeMode) {
            TimeOfDayMode.DAY -> 0.25f
            TimeOfDayMode.SUNSET -> 0.52f
            TimeOfDayMode.NIGHT -> 0.85f
            TimeOfDayMode.DYNAMIC -> (timeOfDayNormalized + dt * 0.0045f) % 1.0f
        }

        if (!isPlaying) {
            // Showcase orbit in Main Menu
            menuOrbitAngle += dt * 0.28f
            val focusX = -22f
            val focusZ = -16f
            camTarget.set(focusX, 1.35f, focusZ)
            camPos.set(
                focusX + sinF(menuOrbitAngle) * 7.5f,
                3.0f + sinF(menuOrbitAngle * 0.5f) * 0.6f,
                focusZ + cosF(menuOrbitAngle) * 7.5f
            )
            camFov = 54f
            playerPose.animState = CharacterAnimState.IDLE
            playerPose.animTime = worldTimeSec
            partnerPose.animState = CharacterAnimState.IDLE
            partnerPose.animTime = worldTimeSec + 1.1f
            stepTraffic(dt)
            return
        }

        if (isPaused || missionCompletePoints != null || isGameOver) {
            soundEngine.updateVehicleAudio(false, 0f, false)
            return
        }

        // Timers
        if (dialogueTimer > 0f) {
            dialogueTimer -= dt
            if (dialogueTimer <= 0f) dialogueKey = null
        }
        if (cinematicTimer > 0f) cinematicTimer -= dt
        if (arrestCamTimer > 0f) arrestCamTimer -= dt
        if (actionAnimCooldown > 0f) actionAnimCooldown -= dt

        // Mission countdown
        if (activeMissionId > 0) {
            missionTimeRemaining -= dt
            if (missionTimeRemaining <= 0f) {
                missionTimeRemaining = 0f
                isGameOver = true
                soundEngine.updateVehicleAudio(false, 0f, false)
                soundEngine.playGameOver()
                return
            }
        }

        // Apply touch camera orbit deltas
        if (abs(input.camOrbitDeltaYaw) > 0.0001f || abs(input.camOrbitDeltaPitch) > 0.0001f) {
            camYawOffset = Math3D.normalizeAngle(camYawOffset - input.camOrbitDeltaYaw * 0.0055f)
            camPitch = (camPitch + input.camOrbitDeltaPitch * 0.0035f).coerceIn(0.14f, 0.78f)
            input.camOrbitDeltaYaw = 0f
            input.camOrbitDeltaPitch = 0f
        }

        // Player control: on foot vs driving
        val activeVehicle = drivenVehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }
        if (activeVehicle != null) {
            stepPlayerDriving(activeVehicle, dt)
        } else {
            stepPlayerOnFoot(dt)
            soundEngine.updateVehicleAudio(false, 0f, false)
        }

        // Partner AI gently follows when on foot
        stepPartnerFollow(dt)

        // Traffic & Civilians & Criminals
        stepTraffic(dt)
        stepCivilians(dt)
        stepCriminals(dt)

        // Mission checkpoints
        stepMissionLogic(dt)

        // Camera update
        updateGameplayCamera(activeVehicle, dt)
    }

    private fun stepPlayerOnFoot(dt: Float) {
        val jx = input.joyX
        val jy = input.joyY
        val mag = sqrt(jx * jx + jy * jy).coerceIn(0f, 1f)

        val wantRun = input.isRunning || mag > 0.86f
        val canRun = wantRun && stamina > 3f && mag > 0.1f
        if (canRun) {
            stamina = (stamina - dt * 16f).coerceAtLeast(0f)
        } else {
            stamina = (stamina + dt * 20f).coerceAtMost(100f)
        }

        val baseSpeed = if (activeOfficer == OfficerId.PATLU) 5.6f else 4.9f
        val runMult = if (activeOfficer == OfficerId.PATLU) 1.72f else 1.58f
        val speed = if (mag < 0.08f) 0f else baseSpeed * mag * (if (canRun) runMult else 1.0f)

        if (speed > 0.05f && cinematicTimer <= 0f) {
            // Camera-relative movement direction
            val camForwardX = sinF(camYawOffset)
            val camForwardZ = cosF(camYawOffset)
            val camRightX = cosF(camYawOffset)
            val camRightZ = -sinF(camYawOffset)

            // Note: joystick up is jy < 0 in screen coords, down is jy > 0
            val moveX = camRightX * jx + camForwardX * (-jy)
            val moveZ = camRightZ * jx + camForwardZ * (-jy)
            val len = sqrt(moveX * moveX + moveZ * moveZ).coerceAtLeast(0.001f)
            val dirX = moveX / len
            val dirZ = moveZ / len

            val targetYaw = atan2(dirX, dirZ)
            val prevYaw = playerYaw
            playerYaw = Math3D.lerpAngle(playerYaw, targetYaw, (dt * 12f).coerceAtMost(1f))
            playerPose.leanAngleRad = Math3D.normalizeAngle(targetYaw - prevYaw) * 0.45f

            playerPos.x = (playerPos.x + dirX * speed * dt).coerceIn(-94f, 94f)
            playerPos.z = (playerPos.z + dirZ * speed * dt).coerceIn(-94f, 94f)
            Math3D.pushOutCircleVsBoxes(playerPos, 0.52f, world.colliders)

            if (isGrounded) {
                footstepCooldown -= dt
                if (footstepCooldown <= 0f) {
                    soundEngine.playFootstep(canRun)
                    footstepCooldown = if (canRun) 0.27f else 0.42f
                }
            }
        } else {
            playerPose.leanAngleRad = Math3D.smoothDamp(playerPose.leanAngleRad, 0f, 10f, dt)
        }

        // Gravity & Jump
        if (!isGrounded) {
            playerVelY -= 21.5f * dt
            playerPos.y += playerVelY * dt
            if (playerPos.y <= 0f) {
                playerPos.y = 0f
                playerVelY = 0f
                isGrounded = true
            }
        }

        // Animation State
        playerPose.animTime += dt
        playerPose.animState = when {
            actionAnimCooldown > 0f -> CharacterAnimState.POLICE_ACTION
            !isGrounded -> CharacterAnimState.JUMP
            speed > 5.5f -> CharacterAnimState.RUN
            speed > 0.2f -> CharacterAnimState.WALK
            dialogueTimer > 0.5f -> CharacterAnimState.TALKING
            else -> CharacterAnimState.IDLE
        }
    }

    private fun stepPlayerDriving(v: VehicleEntity, dt: Float) {
        val throttleInput = when {
            input.gasPressed -> 1f
            input.brakePressed -> -1f
            abs(input.joyY) > 0.15f -> -input.joyY
            else -> 0f
        }
        val steerInput = input.joyX.coerceIn(-1f, 1f)

        val maxFwd = if (v.type == VehicleType.POLICE_SUV) 24f else 27f
        val maxRev = -9f
        val accel = 18f
        val drag = 5.2f

        if (throttleInput > 0.05f) {
            v.speed += throttleInput * accel * dt
        } else if (throttleInput < -0.05f) {
            if (v.speed > 1f) {
                v.speed -= 28f * dt // strong braking
            } else {
                v.speed += throttleInput * 12f * dt
            }
        } else {
            // Natural rolling drag
            v.speed = Math3D.smoothDamp(v.speed, 0f, drag * 0.45f, dt)
        }
        v.speed = v.speed.coerceIn(maxRev, maxFwd)

        val turnRate = if (activeOfficer == OfficerId.PATLU) 2.35f else 2.05f
        val speedFactor = (abs(v.speed) / 8f).coerceIn(0f, 1f)
        val dirSign = if (v.speed >= -0.1f) 1f else -1f
        v.steerAngleRad = Math3D.smoothDamp(v.steerAngleRad, -steerInput * 0.48f, 12f, dt)
        v.yawRad = Math3D.normalizeAngle(v.yawRad - steerInput * turnRate * speedFactor * dirSign * dt)

        val fwdX = sinF(v.yawRad)
        val fwdZ = cosF(v.yawRad)
        v.pos.x = (v.pos.x + fwdX * v.speed * dt).coerceIn(-92f, 92f)
        v.pos.z = (v.pos.z + fwdZ * v.speed * dt).coerceIn(-92f, 92f)
        v.wheelSpinRad += v.speed * dt * 2.4f

        if (Math3D.pushOutCircleVsBoxes(v.pos, 1.55f, world.colliders)) {
            v.speed *= -0.25f
        }

        // Check ramming getaway vehicle in Mission 2
        getawayVehicleId?.let { gid ->
            val gv = vehicles.firstOrNull { it.id == gid }
            if (gv != null && v.pos.distXZ(gv.pos.x, gv.pos.z) < 3.6f) {
                gv.speed *= 0.2f
                if (activeMissionId == 2 && objectiveStep == 1) {
                    gv.speed = 0f
                    sessionScore += 200
                    soundEngine.playArrest()
                    triggerDialogue("dialogue_arrest", 2.5f)
                }
            }
        }

        playerPos.set(v.pos.x, v.pos.y, v.pos.z)
        playerYaw = v.yawRad
        soundEngine.updateVehicleAudio(true, abs(v.speed) / maxFwd, v.sirenOn)
    }

    private fun stepPartnerFollow(dt: Float) {
        if (drivenVehicleId != null) {
            partnerPos.set(-28f, 0f, -18f)
            partnerPose.animState = CharacterAnimState.IDLE
            return
        }
        val targetX = playerPos.x - cosF(playerYaw) * 2.1f - sinF(playerYaw) * 1.4f
        val targetZ = playerPos.z + sinF(playerYaw) * 2.1f - cosF(playerYaw) * 1.4f
        val dx = targetX - partnerPos.x
        val dz = targetZ - partnerPos.z
        val dist = sqrt(dx * dx + dz * dz)
        partnerPose.animTime += dt
        if (dist > 1.8f && dist < 28f) {
            val moveSpd = if (dist > 5.5f) 6.6f else 3.8f
            partnerPos.x += (dx / dist) * moveSpd * dt
            partnerPos.z += (dz / dist) * moveSpd * dt
            Math3D.pushOutCircleVsBoxes(partnerPos, 0.45f, world.colliders)
            partnerYaw = Math3D.lerpAngle(partnerYaw, atan2(dx, dz), (dt * 8f).coerceAtMost(1f))
            partnerPose.animState = if (moveSpd > 5f) CharacterAnimState.RUN else CharacterAnimState.WALK
        } else if (dist >= 28f) {
            partnerPos.set(targetX, 0f, targetZ)
            partnerPose.animState = CharacterAnimState.IDLE
        } else {
            partnerPose.animState = CharacterAnimState.IDLE
        }
    }

    private fun stepTraffic(dt: Float) {
        val playerV = drivenVehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }
        val sirenActive = playerV?.sirenOn == true

        for (v in vehicles) {
            if (v.occupiedByPlayer) continue
            if (v.type == VehicleType.POLICE_CRUISER || v.type == VehicleType.POLICE_SUV) continue

            if (v.id == getawayVehicleId) {
                if (v.speed < 0.5f) continue // stopped after intercept!
                val loop = world.trafficLoops[0]
                val wp = loop[v.waypointIndex % loop.size]
                val dx = wp.x - v.pos.x
                val dz = wp.z - v.pos.z
                val d = sqrt(dx * dx + dz * dz)
                if (d < 5f) {
                    v.waypointIndex = (v.waypointIndex + 1) % loop.size
                } else {
                    val targetYaw = atan2(dx, dz)
                    v.yawRad = Math3D.lerpAngle(v.yawRad, targetYaw, (dt * 4f).coerceAtMost(1f))
                    v.pos.x += sinF(v.yawRad) * v.speed * dt
                    v.pos.z += cosF(v.yawRad) * v.speed * dt
                    v.wheelSpinRad += v.speed * dt * 2.4f
                }
                continue
            }

            val loopIdx = v.id % world.trafficLoops.size
            val loop = world.trafficLoops[loopIdx]
            val wp = loop[v.waypointIndex % loop.size]
            val dx = wp.x - v.pos.x
            val dz = wp.z - v.pos.z
            val dist = sqrt(dx * dx + dz * dz)
            if (dist < 4.5f) {
                v.waypointIndex = (v.waypointIndex + 1) % loop.size
            } else {
                val yieldFactor = if (sirenActive && v.pos.distXZ(playerPos.x, playerPos.z) < 20f) 0.25f else 1.0f
                val targetSpeed = 8.5f * yieldFactor
                v.speed = Math3D.smoothDamp(v.speed, targetSpeed, 4f, dt)
                val targetYaw = atan2(dx, dz)
                v.yawRad = Math3D.lerpAngle(v.yawRad, targetYaw, (dt * 4.5f).coerceAtMost(1f))
                v.pos.x += sinF(v.yawRad) * v.speed * dt
                v.pos.z += cosF(v.yawRad) * v.speed * dt
                v.wheelSpinRad += v.speed * dt * 2.4f
            }
        }
    }

    private fun stepCivilians(dt: Float) {
        for (c in civilians) {
            c.pose.animTime += dt
            if (c.isActiveTarget) {
                // Static mission NPC waiting near checkpoint
                val dx = playerPos.x - c.pos.x
                val dz = playerPos.z - c.pos.z
                if (dx * dx + dz * dz < 64f) {
                    c.yawRad = Math3D.lerpAngle(c.yawRad, atan2(dx, dz), (dt * 4f).coerceAtMost(1f))
                }
                if (c.isProtectedChecked) {
                    c.pose.animState = CharacterAnimState.TALKING
                    c.pose.isTalking = true
                } else {
                    c.pose.animState = CharacterAnimState.IDLE
                }
                continue
            }

            val loop = world.pedestrianLoops[c.loopIndex % world.pedestrianLoops.size]
            val wp = loop[c.waypointIndex % loop.size]
            val dx = wp.x - c.pos.x
            val dz = wp.z - c.pos.z
            val d = sqrt(dx * dx + dz * dz)
            if (d < 1.5f) {
                c.waypointIndex = (c.waypointIndex + 1) % loop.size
            } else {
                val spd = 2.0f
                c.pos.x += (dx / d) * spd * dt
                c.pos.z += (dz / d) * spd * dt
                c.yawRad = Math3D.lerpAngle(c.yawRad, atan2(dx, dz), (dt * 6f).coerceAtMost(1f))
                c.pose.animState = CharacterAnimState.WALK
            }
        }
    }

    private fun stepCriminals(dt: Float) {
        for (cr in criminals) {
            cr.pose.animTime += dt
            if (cr.isApprehended) {
                cr.pose.animState = CharacterAnimState.IDLE
                continue
            }
            if (!cr.isActiveTarget) {
                cr.pose.animState = CharacterAnimState.IDLE
                continue
            }
            val dx = cr.pos.x - playerPos.x
            val dz = cr.pos.z - playerPos.z
            val dist = sqrt(dx * dx + dz * dz)
            if (dist < 28f) {
                // Flee away from police officer + gentle zig-zag around district
                val angle = atan2(dx, dz) + sinF(worldTimeSec * 1.4f + cr.id) * 0.45f
                val fx = sinF(angle)
                val fz = cosF(angle)
                cr.pos.x = (cr.pos.x + fx * cr.fleeSpeed * dt).coerceIn(-54f, 54f)
                cr.pos.z = (cr.pos.z + fz * cr.fleeSpeed * dt).coerceIn(-54f, 54f)
                Math3D.pushOutCircleVsBoxes(cr.pos, 0.5f, world.colliders)
                cr.yawRad = Math3D.lerpAngle(cr.yawRad, atan2(fx, fz), (dt * 8f).coerceAtMost(1f))
                cr.pose.animState = CharacterAnimState.RUN
            } else {
                cr.pose.animState = CharacterAnimState.IDLE
            }
        }
    }

    private fun stepMissionLogic(dt: Float) {
        val cp = checkpointPos
        if (checkpointVisible && cp != null && playerPos.distXZ(cp.x, cp.z) < 4.5f) {
            when (activeMissionId) {
                1 -> if (objectiveStep == 0) {
                    objectiveStep = 1
                    checkpointVisible = false
                    criminals.firstOrNull()?.isActiveTarget = true
                    soundEngine.playClick()
                }
                3 -> if (objectiveStep == 0) {
                    objectiveStep = 1
                    checkpointVisible = false
                    criminals.forEach { it.isActiveTarget = true }
                    soundEngine.playClick()
                } else if (objectiveStep == 2) {
                    completeMission()
                }
                4 -> if (objectiveStep == 0) {
                    objectiveStep = 1
                    checkpointVisible = false
                    criminals.firstOrNull()?.isActiveTarget = true
                    soundEngine.playClick()
                } else if (objectiveStep == 2) {
                    completeMission()
                }
                5 -> if (objectiveStep == 0) {
                    objectiveStep = 1
                    checkpointPos = Vec3(-30f, 0f, 30f) // Emerald Park next
                    soundEngine.playClick()
                } else if (objectiveStep == 1) {
                    objectiveStep = 2
                    checkpointVisible = false
                    criminals.firstOrNull()?.isActiveTarget = true
                    soundEngine.playClick()
                }
            }
        }

        // Also update checkpoint marker dynamically to track active criminal in Mission 2/4
        if (activeMissionId == 2 && objectiveStep == 1) {
            val gv = vehicles.firstOrNull { it.id == getawayVehicleId }
            if (gv != null && gv.speed < 1f) {
                // Ready to exit & arrest
                checkpointPos = Vec3(gv.pos.x, 0f, gv.pos.z)
                checkpointVisible = true
            }
        }
    }

    private fun updateGameplayCamera(activeVehicle: VehicleEntity?, dt: Float) {
        if (cinematicTimer > 0f) {
            // Sweeping cinematic mission intro camera
            val progress = 1f - (cinematicTimer / 2.2f)
            val orbit = playerYaw + 2.4f - progress * 2.4f
            val dist = 6.2f + (1f - progress) * 3.5f
            camTarget.set(playerPos.x, 1.45f, playerPos.z)
            camPos.set(
                playerPos.x - sinF(orbit) * dist,
                2.8f + (1f - progress) * 1.8f,
                playerPos.z - cosF(orbit) * dist
            )
            camFov = 52f
            return
        }

        if (arrestCamTimer > 0f) {
            val sideAngle = playerYaw + 0.85f
            camTarget.set(playerPos.x, 1.25f, playerPos.z)
            camPos.set(
                Math3D.smoothDamp(camPos.x, playerPos.x - sinF(sideAngle) * 3.8f, 9f, dt),
                Math3D.smoothDamp(camPos.y, 1.9f, 9f, dt),
                Math3D.smoothDamp(camPos.z, playerPos.z - cosF(sideAngle) * 3.8f, 9f, dt)
            )
            camFov = Math3D.smoothDamp(camFov, 48f, 8f, dt)
            return
        }

        val isDriving = activeVehicle != null
        val speedNorm = if (activeVehicle != null) (abs(activeVehicle.speed) / 26f).coerceIn(0f, 1f) else 0f

        // Gently auto-align camera behind vehicle when driving
        if (isDriving && abs(activeVehicle!!.speed) > 2f) {
            camYawOffset = Math3D.lerpAngle(camYawOffset, activeVehicle.yawRad, (dt * 2.8f).coerceAtMost(1f))
        }

        val followDist = if (isDriving) 7.4f + speedNorm * 1.8f else 5.2f
        val followHeight = if (isDriving) 2.9f + camPitch * 2.5f else 2.0f + camPitch * 3.0f
        val shake = if (isDriving && speedNorm > 0.65f) sinF(worldTimeSec * 36f) * 0.035f * speedNorm else 0f

        val desiredCamX = playerPos.x - sinF(camYawOffset) * followDist + shake
        val desiredCamZ = playerPos.z - cosF(camYawOffset) * followDist
        val desiredCamY = (playerPos.y + followHeight).coerceAtLeast(1.4f)

        camPos.x = Math3D.smoothDamp(camPos.x, desiredCamX, 9f, dt)
        camPos.y = Math3D.smoothDamp(camPos.y, desiredCamY, 8f, dt)
        camPos.z = Math3D.smoothDamp(camPos.z, desiredCamZ, 9f, dt)

        camTarget.x = Math3D.smoothDamp(camTarget.x, playerPos.x, 12f, dt)
        camTarget.y = Math3D.smoothDamp(camTarget.y, playerPos.y + (if (isDriving) 1.25f else 1.45f), 12f, dt)
        camTarget.z = Math3D.smoothDamp(camTarget.z, playerPos.z, 12f, dt)

        val targetFov = if (isDriving) 60f + speedNorm * 10f else 56f
        camFov = Math3D.smoothDamp(camFov, targetFov, 6f, dt)
    }

    private fun resetWorldEntities() {
        vehicles.clear()
        civilians.clear()
        criminals.clear()

        // Parked Police Vehicles around Precinct & Park
        vehicles.add(VehicleEntity(1, VehicleType.POLICE_CRUISER, Vec3(-14f, 0f, -16f), yawRad = 0.0f))
        vehicles.add(VehicleEntity(2, VehicleType.POLICE_SUV, Vec3(-14f, 0f, -24f), yawRad = 0.0f))
        vehicles.add(VehicleEntity(3, VehicleType.POLICE_CRUISER, Vec3(-8f, 0f, 18f), yawRad = 3.14f))

        // Living City Traffic
        vehicles.add(VehicleEntity(10, VehicleType.CITY_TAXI, Vec3(-63.5f, 0f, -20f), yawRad = 0f, waypointIndex = 1))
        vehicles.add(VehicleEntity(11, VehicleType.CIVILIAN_SEDAN, Vec3(63.5f, 0f, 15f), yawRad = 3.14f, waypointIndex = 3))
        vehicles.add(VehicleEntity(12, VehicleType.CIVILIAN_SEDAN, Vec3(20f, 0f, -56.5f), yawRad = 1.57f, waypointIndex = 1))
        vehicles.add(VehicleEntity(13, VehicleType.CITY_TAXI, Vec3(-25f, 0f, 56.5f), yawRad = -1.57f, waypointIndex = 3))
        vehicles.add(VehicleEntity(14, VehicleType.CIVILIAN_SEDAN, Vec3(3.5f, 0f, -12f), yawRad = 3.14f, waypointIndex = 1))

        // Sidewalk Civilians
        for (i in 0 until 10) {
            val loopIdx = i % world.pedestrianLoops.size
            val loop = world.pedestrianLoops[loopIdx]
            val wp = loop[i % loop.size]
            civilians.add(
                NpcPerson(
                    id = i + 1,
                    role = CharacterRole.CIVILIAN,
                    pos = Vec3(wp.x + (i % 3) * 1.1f, 0f, wp.z - (i % 2) * 1.1f),
                    yawRad = i * 0.8f,
                    loopIndex = loopIdx,
                    waypointIndex = (i + 1) % loop.size
                )
            )
        }
    }

    private fun spawnCriminal(id: Int, pos: Vec3, fleeSpeed: Float, active: Boolean) {
        criminals.add(
            NpcPerson(
                id = id,
                role = CharacterRole.CRIMINAL,
                pos = pos,
                yawRad = 0f,
                fleeSpeed = fleeSpeed,
                isActiveTarget = active
            )
        )
    }

    private fun findNearestEnterableVehicle(): VehicleEntity? {
        var best: VehicleEntity? = null
        var bestDist = 4.2f
        for (v in vehicles) {
            if (v.id == getawayVehicleId) continue
            val d = playerPos.distXZ(v.pos.x, v.pos.z)
            if (d < bestDist) {
                bestDist = d
                best = v
            }
        }
        return best
    }

    private fun triggerDialogue(key: String, duration: Float) {
        dialogueKey = key
        dialogueTimer = duration
    }

    fun activeRole(): CharacterRole =
        if (activeOfficer == OfficerId.MOTU) CharacterRole.MOTU else CharacterRole.PATLU

    fun partnerRole(): CharacterRole =
        if (activeOfficer == OfficerId.MOTU) CharacterRole.PATLU else CharacterRole.MOTU

    fun buildHudSnapshot(): HudSnapshot {
        val activeV = drivenVehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }
        val nearEnterable = if (activeV == null) findNearestEnterableVehicle() != null else false

        val arrestRange = if (activeOfficer == OfficerId.MOTU) 3.8f else 3.2f
        val actionCtx = when {
            activeV != null -> ActionContextType.NONE
            criminals.any { !it.isApprehended && it.isActiveTarget && playerPos.distXZ(it.pos.x, it.pos.z) <= arrestRange } -> ActionContextType.ARREST
            civilians.any { playerPos.distXZ(it.pos.x, it.pos.z) <= 3.6f && (it.isActiveTarget || !it.isProtectedChecked) } -> ActionContextType.TALK
            else -> ActionContextType.NONE
        }

        val blips = ArrayList<RadarBlip>(16)
        blips.add(RadarBlip(-30f, -34f, BlipType.PRECINCT))
        checkpointPos?.takeIf { checkpointVisible }?.let {
            blips.add(RadarBlip(it.x, it.z, BlipType.OBJECTIVE))
        }
        for (cr in criminals) {
            if (!cr.isApprehended && cr.isActiveTarget) {
                blips.add(RadarBlip(cr.pos.x, cr.pos.z, BlipType.CRIMINAL))
            }
        }
        getawayVehicleId?.let { gid ->
            vehicles.firstOrNull { it.id == gid && it.speed > 0.5f }?.let {
                blips.add(RadarBlip(it.pos.x, it.pos.z, BlipType.CRIMINAL))
            }
        }
        for (v in vehicles) {
            if (!v.occupiedByPlayer && (v.type == VehicleType.POLICE_CRUISER || v.type == VehicleType.POLICE_SUV)) {
                blips.add(RadarBlip(v.pos.x, v.pos.z, BlipType.POLICE_CAR))
            }
        }

        val objKey = when (activeMissionId) {
            1 -> when (objectiveStep) {
                0 -> "m1_1"; 1 -> "m1_2"; else -> "m1_3"
            }
            2 -> when (objectiveStep) {
                0 -> "m2_1"; 1 -> "m2_2"; else -> "m2_3"
            }
            3 -> when (objectiveStep) {
                0 -> "m3_1"; 1 -> "m3_2"; else -> "m3_3"
            }
            4 -> when (objectiveStep) {
                0 -> "m4_1"; 1 -> "m4_2"; else -> "m4_3"
            }
            5 -> when (objectiveStep) {
                0 -> "m5_1"; 1 -> "m5_2"; else -> "m5_3"
            }
            else -> "free"
        }

        return HudSnapshot(
            activeMissionId = activeMissionId,
            objectiveStep = objectiveStep,
            objectiveTextKey = objKey,
            district = world.getDistrictAt(playerPos.x, playerPos.z),
            officer = activeOfficer,
            health = health,
            stamina = stamina,
            scoreInSession = sessionScore,
            timeRemainingSec = missionTimeRemaining,
            isInVehicle = activeV != null,
            canEnterVehicle = nearEnterable,
            sirenOn = activeV?.sirenOn ?: false,
            speedKmh = ((activeV?.speed ?: 0f) * 3.8f).toInt().let { abs(it) },
            actionContext = actionCtx,
            playerX = playerPos.x,
            playerZ = playerPos.z,
            playerYawRad = playerYaw,
            radarBlips = blips,
            timeOfDayNormalized = timeOfDayNormalized,
            dialogueText = dialogueKey,
            isCinematicIntro = cinematicTimer > 0f,
            missionCompletePoints = missionCompletePoints,
            missionCompleteBonus = missionCompleteBonus,
            missionCompleteStars = missionCompleteStars,
            isGameOver = isGameOver,
            isPaused = isPaused
        )
    }
}
