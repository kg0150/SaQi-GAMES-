package com.example.game3d

import kotlin.math.abs
import kotlin.math.max

enum class CharacterAnimState {
    IDLE,
    WALK,
    RUN,
    JUMP,
    DRIVING,
    ENTER_EXIT,
    TALKING,
    POLICE_ACTION
}

enum class CharacterRole {
    MOTU,
    PATLU,
    CIVILIAN,
    CRIMINAL
}

data class CharacterPose(
    var animState: CharacterAnimState = CharacterAnimState.IDLE,
    var animTime: Float = 0f,
    var moveSpeedNorm: Float = 0f,
    var actionTimer: Float = 0f,
    var jumpPhase: Float = 0f,
    var isTalking: Boolean = false,
    var leanAngleRad: Float = 0f
)

class SharedMeshes {
    lateinit var box: GpuMesh
    lateinit var groundQuad: GpuMesh
    lateinit var sphere: GpuMesh
    lateinit var cylinder: GpuMesh
    lateinit var coneTaper: GpuMesh
    lateinit var capsule: GpuMesh

    fun init() {
        box = MeshBuilder.createBox()
        groundQuad = MeshBuilder.createGroundQuad()
        sphere = MeshBuilder.createSphere(rings = 16, sectors = 22)
        cylinder = MeshBuilder.createTaperedCylinder(segments = 20, rBottom = 0.5f, rTop = 0.5f)
        coneTaper = MeshBuilder.createTaperedCylinder(segments = 18, rBottom = 0.58f, rTop = 0.36f)
        capsule = MeshBuilder.createCapsule(segments = 16, rings = 10, bodyHeight = 0.6f, radius = 0.32f)
    }

    fun release() {
        box.release()
        groundQuad.release()
        sphere.release()
        cylinder.release()
        coneTaper.release()
        capsule.release()
    }
}

class MaterialUniforms(
    var r: Float = 1f,
    var g: Float = 1f,
    var b: Float = 1f,
    var a: Float = 1f,
    var emissiveR: Float = 0f,
    var emissiveG: Float = 0f,
    var emissiveB: Float = 0f,
    var emissiveStrength: Float = 0f,
    var specular: Float = 0.25f,
    var shininess: Float = 24f,
    var rim: Float = 0.16f,
    var uvScaleX: Float = 1f,
    var uvScaleY: Float = 1f,
    var nightWindowBoost: Float = 0f
)

interface DrawSubmitter {
    fun drawMesh(
        mesh: GpuMesh,
        modelMatrix: FloatArray,
        textureId: Int,
        mat: MaterialUniforms
    )
}

class Character3DRenderer(
    private val meshes: SharedMeshes,
    private val textures: TextureSet
) {
    private val stack = MatrixStack(20)
    private val mat = MaterialUniforms()

    fun renderCharacter(
        submitter: DrawSubmitter,
        role: CharacterRole,
        x: Float,
        y: Float,
        z: Float,
        yawRad: Float,
        pose: CharacterPose,
        variantSeed: Int = 0
    ) {
        val isMotu = role == CharacterRole.MOTU
        val isPatlu = role == CharacterRole.PATLU
        val isPolice = isMotu || isPatlu
        val isCriminal = role == CharacterRole.CRIMINAL

        // Proportions calibrated so Motu is short/stocky/muscular and Patlu is tall/slim
        val heightScale = when (role) {
            CharacterRole.MOTU -> 0.92f
            CharacterRole.PATLU -> 1.07f
            CharacterRole.CRIMINAL -> 1.00f
            CharacterRole.CIVILIAN -> 0.96f + (variantSeed % 3) * 0.03f
        }
        val torsoWidth = if (isMotu) 0.56f else if (isPatlu) 0.37f else 0.43f
        val torsoDepth = if (isMotu) 0.40f else if (isPatlu) 0.24f else 0.28f
        val limbThick = if (isMotu) 0.155f else if (isPatlu) 0.095f else 0.115f
        val headScale = if (isMotu) 0.40f else if (isPatlu) 0.35f else 0.36f

        // Skin palette: Motu dark/black, Patlu very fair/light
        val skinR: Float
        val skinG: Float
        val skinB: Float
        when (role) {
            CharacterRole.MOTU -> {
                skinR = 0.23f; skinG = 0.14f; skinB = 0.10f
            }
            CharacterRole.PATLU -> {
                skinR = 0.96f; skinG = 0.81f; skinB = 0.70f
            }
            CharacterRole.CRIMINAL -> {
                skinR = 0.62f; skinG = 0.45f; skinB = 0.35f
            }
            CharacterRole.CIVILIAN -> {
                when (variantSeed % 3) {
                    0 -> { skinR = 0.78f; skinG = 0.58f; skinB = 0.44f }
                    1 -> { skinR = 0.48f; skinG = 0.32f; skinB = 0.24f }
                    else -> { skinR = 0.91f; skinG = 0.74f; skinB = 0.62f }
                }
            }
        }

        // Clothing colors
        val shirtR: Float; val shirtG: Float; val shirtB: Float
        val pantsR: Float; val pantsG: Float; val pantsB: Float
        when {
            isPolice -> {
                shirtR = 0.10f; shirtG = 0.18f; shirtB = 0.36f
                pantsR = 0.07f; pantsG = 0.12f; pantsB = 0.24f
            }
            isCriminal -> {
                shirtR = 0.18f; shirtG = 0.17f; shirtB = 0.20f
                pantsR = 0.12f; pantsG = 0.13f; pantsB = 0.16f
            }
            else -> {
                when (variantSeed % 4) {
                    0 -> { shirtR = 0.76f; shirtG = 0.34f; shirtB = 0.24f }
                    1 -> { shirtR = 0.18f; shirtG = 0.56f; shirtB = 0.46f }
                    2 -> { shirtR = 0.84f; shirtG = 0.66f; shirtB = 0.24f }
                    else -> { shirtR = 0.36f; shirtG = 0.46f; shirtB = 0.72f }
                }
                pantsR = 0.20f; pantsG = 0.24f; pantsB = 0.30f
            }
        }

        // Compute procedural joint angles from animState
        val t = pose.animTime
        var hipBob = 0f
        var torsoPitchDeg = 0f
        var torsoYawDeg = 0f
        var leftLegSwingDeg = 0f
        var rightLegSwingDeg = 0f
        var leftKneeBendDeg = 0f
        var rightKneeBendDeg = 0f
        var leftArmSwingDeg = 0f
        var rightArmSwingDeg = 0f
        var leftElbowBendDeg = -10f
        var rightElbowBendDeg = -10f
        var headNodDeg = 0f
        var headTurnDeg = 0f

        when (pose.animState) {
            CharacterAnimState.IDLE -> {
                val breath = sinF(t * 2.4f)
                hipBob = breath * 0.014f
                leftArmSwingDeg = 3f + breath * 2.5f
                rightArmSwingDeg = -3f - breath * 2.5f
                leftElbowBendDeg = -12f
                rightElbowBendDeg = -12f
                headTurnDeg = sinF(t * 0.9f) * 7f
                headNodDeg = breath * 2f
            }
            CharacterAnimState.WALK -> {
                val freq = 7.2f
                val s = sinF(t * freq)
                val c = cosF(t * freq)
                hipBob = abs(s) * 0.045f
                torsoPitchDeg = 4f
                torsoYawDeg = s * 5f
                leftLegSwingDeg = s * 32f
                rightLegSwingDeg = -s * 32f
                leftKneeBendDeg = max(0f, -c) * 28f
                rightKneeBendDeg = max(0f, c) * 28f
                leftArmSwingDeg = -s * 26f
                rightArmSwingDeg = s * 26f
                leftElbowBendDeg = -18f - max(0f, -s) * 14f
                rightElbowBendDeg = -18f - max(0f, s) * 14f
            }
            CharacterAnimState.RUN -> {
                val freq = 11.8f
                val s = sinF(t * freq)
                val c = cosF(t * freq)
                hipBob = abs(s) * 0.085f
                torsoPitchDeg = 14f
                torsoYawDeg = s * 9f
                leftLegSwingDeg = s * 54f
                rightLegSwingDeg = -s * 54f
                leftKneeBendDeg = max(0f, -c) * 58f
                rightKneeBendDeg = max(0f, c) * 58f
                leftArmSwingDeg = -s * 48f
                rightArmSwingDeg = s * 48f
                leftElbowBendDeg = -64f
                rightElbowBendDeg = -64f
                headNodDeg = -8f
            }
            CharacterAnimState.JUMP -> {
                hipBob = 0.04f
                torsoPitchDeg = 8f
                leftLegSwingDeg = -22f
                rightLegSwingDeg = 18f
                leftKneeBendDeg = 42f
                rightKneeBendDeg = 28f
                leftArmSwingDeg = -55f
                rightArmSwingDeg = -55f
                leftElbowBendDeg = -32f
                rightElbowBendDeg = -32f
            }
            CharacterAnimState.DRIVING -> {
                hipBob = -0.34f
                torsoPitchDeg = -8f
                leftLegSwingDeg = -78f
                rightLegSwingDeg = -78f
                leftKneeBendDeg = 72f
                rightKneeBendDeg = 72f
                leftArmSwingDeg = -62f
                rightArmSwingDeg = -62f
                leftElbowBendDeg = -25f
                rightElbowBendDeg = -25f
                headTurnDeg = (pose.leanAngleRad * Math3D.RAD_TO_DEG).coerceIn(-18f, 18f)
            }
            CharacterAnimState.ENTER_EXIT -> {
                val s = sinF(t * 8f)
                hipBob = 0.05f
                leftLegSwingDeg = -35f + s * 15f
                rightLegSwingDeg = 15f
                leftArmSwingDeg = -45f
                rightArmSwingDeg = -25f
            }
            CharacterAnimState.TALKING -> {
                val g = sinF(t * 5.2f)
                hipBob = sinF(t * 2.2f) * 0.015f
                leftArmSwingDeg = -28f + g * 18f
                rightArmSwingDeg = -34f - g * 16f
                leftElbowBendDeg = -68f + g * 14f
                rightElbowBendDeg = -74f - g * 14f
                headNodDeg = sinF(t * 4.1f) * 6f
                headTurnDeg = cosF(t * 2.3f) * 8f
            }
            CharacterAnimState.POLICE_ACTION -> {
                val p = (pose.actionTimer / 0.45f).coerceIn(0f, 1f)
                val strike = sinF(p * 3.14159f)
                torsoPitchDeg = 18f * strike
                torsoYawDeg = -24f * strike
                rightArmSwingDeg = -96f * strike - 15f
                rightElbowBendDeg = -16f
                leftArmSwingDeg = 24f * strike
                leftElbowBendDeg = -45f
                leftLegSwingDeg = -26f * strike
                rightLegSwingDeg = 22f * strike
            }
        }

        // Blink & mouth animations
        val blinkCycle = t % 3.8f
        val eyeOpenY = if (blinkCycle > 3.66f) 0.18f else 1.0f
        val mouthOpen = if (pose.isTalking || pose.animState == CharacterAnimState.TALKING) {
            0.35f + abs(sinF(t * 11f)) * 0.85f
        } else 0.16f

        stack.reset()
        stack.push()
        stack.translate(x, y, z)
        stack.rotateYRad(yawRad)
        stack.rotateDeg((pose.leanAngleRad * Math3D.RAD_TO_DEG).coerceIn(-12f, 12f), 0f, 0f, 1f)
        stack.scale(heightScale, heightScale, heightScale)

        // Pelvis center (~0.84m above feet)
        val pelvisY = 0.84f + hipBob
        stack.translate(0f, pelvisY, 0f)

        // Pelvis mesh
        stack.push()
        stack.scale(torsoWidth * 0.88f, 0.18f, torsoDepth * 0.86f)
        setMat(pantsR, pantsG, pantsB, spec = 0.12f, shiny = 14f)
        submitter.drawMesh(meshes.capsule, stack.current, textures.whiteTex, mat)
        stack.pop()

        // LEGS (Left & Right)
        drawLeg(submitter, -torsoWidth * 0.24f, limbThick, leftLegSwingDeg, leftKneeBendDeg, pantsR, pantsG, pantsB, isPolice)
        drawLeg(submitter, torsoWidth * 0.24f, limbThick, rightLegSwingDeg, rightKneeBendDeg, pantsR, pantsG, pantsB, isPolice)

        // TORSO / UPPER BODY
        stack.push()
        stack.translate(0f, 0.08f, 0f)
        stack.rotateDeg(torsoPitchDeg, 1f, 0f, 0f)
        stack.rotateDeg(torsoYawDeg, 0f, 1f, 0f)

        // Heavy/Slim Chest + Belly sculpting
        if (isMotu) {
            // Motu's stout strong belly + broad barrel chest
            stack.push()
            stack.translate(0f, 0.17f, 0.03f)
            stack.scale(torsoWidth * 1.02f, 0.34f, torsoDepth * 1.12f)
            setMat(shirtR, shirtG, shirtB, spec = 0.18f, shiny = 16f)
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            stack.pop()

            stack.push()
            stack.translate(0f, 0.36f, 0f)
            stack.scale(torsoWidth * 1.06f, 0.32f, torsoDepth * 0.98f)
            setMat(shirtR, shirtG, shirtB, spec = 0.18f, shiny = 16f)
            submitter.drawMesh(meshes.capsule, stack.current, textures.whiteTex, mat)
            stack.pop()
        } else {
            // Patlu's tall slender athletic torso / Civilian / Criminal
            stack.push()
            stack.translate(0f, 0.28f, 0f)
            stack.scale(torsoWidth, 0.50f, torsoDepth)
            setMat(shirtR, shirtG, shirtB, spec = 0.18f, shiny = 16f)
            submitter.drawMesh(meshes.capsule, stack.current, textures.whiteTex, mat)
            stack.pop()
        }

        // Police uniform accessories: Belt, Gold Buckle, Gold Badge, Epaulets, Collar, Radio Mic
        if (isPolice) {
            // Tactical Utility Belt
            stack.push()
            stack.translate(0f, 0.04f, if (isMotu) 0.02f else 0f)
            stack.scale(torsoWidth * (if (isMotu) 1.02f else 0.94f), 0.075f, torsoDepth * (if (isMotu) 1.10f else 0.98f))
            setMat(0.08f, 0.08f, 0.10f, spec = 0.55f, shiny = 42f)
            submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Brass Belt Buckle
            stack.push()
            stack.translate(0f, 0.04f, torsoDepth * (if (isMotu) 0.58f else 0.50f))
            stack.scale(0.09f, 0.065f, 0.04f)
            setMat(0.95f, 0.76f, 0.16f, spec = 0.90f, shiny = 72f)
            submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Metallic Gold Police Star Badge on chest
            stack.push()
            stack.translate(-torsoWidth * 0.24f, 0.41f, torsoDepth * 0.49f)
            stack.scale(0.075f, 0.085f, 0.03f)
            setMat(1.0f, 0.80f, 0.18f, spec = 0.95f, shiny = 80f)
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Radio mic / badge plate on right chest
            stack.push()
            stack.translate(torsoWidth * 0.24f, 0.42f, torsoDepth * 0.49f)
            stack.scale(0.06f, 0.045f, 0.03f)
            setMat(0.14f, 0.16f, 0.20f, spec = 0.5f, shiny = 32f)
            submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Shoulder Epaulets
            for (side in -1..1 step 2) {
                stack.push()
                stack.translate(side * torsoWidth * 0.42f, 0.53f, 0f)
                stack.scale(0.11f, 0.025f, 0.09f)
                setMat(0.05f, 0.09f, 0.22f, spec = 0.4f, shiny = 30f)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                stack.pop()
            }

            // Shirt Collar
            stack.push()
            stack.translate(0f, 0.54f, 0.01f)
            stack.scale(0.20f, 0.06f, 0.18f)
            setMat(0.12f, 0.22f, 0.44f, spec = 0.2f, shiny = 18f)
            submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
            stack.pop()
        } else if (isCriminal) {
            // Criminal crimson chest stripe / bandana collar
            stack.push()
            stack.translate(0f, 0.51f, 0.02f)
            stack.scale(0.24f, 0.08f, 0.21f)
            setMat(0.86f, 0.16f, 0.22f, spec = 0.25f, shiny = 18f)
            submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
            stack.pop()
        }

        // ARMS (Left & Right)
        drawArm(
            submitter,
            side = -1f,
            shoulderX = -torsoWidth * 0.56f,
            limbThick = limbThick,
            armSwingDeg = leftArmSwingDeg,
            elbowBendDeg = leftElbowBendDeg,
            shirtR = shirtR, shirtG = shirtG, shirtB = shirtB,
            skinR = skinR, skinG = skinG, skinB = skinB,
            shortSleeve = isMotu || !isPolice
        )
        drawArm(
            submitter,
            side = 1f,
            shoulderX = torsoWidth * 0.56f,
            limbThick = limbThick,
            armSwingDeg = rightArmSwingDeg,
            elbowBendDeg = rightElbowBendDeg,
            shirtR = shirtR, shirtG = shirtG, shirtB = shirtB,
            skinR = skinR, skinG = skinG, skinB = skinB,
            shortSleeve = isMotu || !isPolice
        )

        // NECK & HEAD
        stack.push()
        stack.translate(0f, 0.57f, 0f)
        // Neck
        stack.push()
        stack.translate(0f, 0.04f, 0f)
        stack.scale(if (isMotu) 0.16f else 0.11f, 0.12f, if (isMotu) 0.15f else 0.11f)
        setMat(skinR, skinG, skinB, spec = 0.22f, shiny = 22f)
        submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Head pivot
        stack.translate(0f, 0.09f + headScale * 0.45f, 0f)
        stack.rotateDeg(headTurnDeg, 0f, 1f, 0f)
        stack.rotateDeg(headNodDeg, 1f, 0f, 0f)

        drawDetailedHead(
            submitter = submitter,
            role = role,
            headScale = headScale,
            skinR = skinR, skinG = skinG, skinB = skinB,
            eyeOpenY = eyeOpenY,
            mouthOpen = mouthOpen,
            isAlert = pose.animState == CharacterAnimState.RUN || pose.animState == CharacterAnimState.POLICE_ACTION
        )

        stack.pop() // neck/head
        stack.pop() // torso
        stack.pop() // root
    }

    private fun drawLeg(
        submitter: DrawSubmitter,
        hipX: Float,
        limbThick: Float,
        hipSwingDeg: Float,
        kneeBendDeg: Float,
        pantsR: Float, pantsG: Float, pantsB: Float,
        isPolice: Boolean
    ) {
        stack.push()
        stack.translate(hipX, -0.02f, 0f)
        stack.rotateDeg(hipSwingDeg, 1f, 0f, 0f)

        // Thigh
        stack.push()
        stack.translate(0f, -0.20f, 0f)
        stack.scale(limbThick * 1.15f, 0.40f, limbThick * 1.15f)
        setMat(pantsR, pantsG, pantsB, spec = 0.12f, shiny = 14f)
        submitter.drawMesh(meshes.capsule, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Knee pivot
        stack.translate(0f, -0.39f, 0f)
        stack.rotateDeg(kneeBendDeg, 1f, 0f, 0f)

        // Shin
        stack.push()
        stack.translate(0f, -0.19f, 0f)
        stack.scale(limbThick * 0.96f, 0.36f, limbThick * 0.96f)
        setMat(pantsR, pantsG, pantsB, spec = 0.12f, shiny = 14f)
        submitter.drawMesh(meshes.capsule, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Tactical Police Boot / Shoe
        stack.push()
        stack.translate(0f, -0.39f, 0.045f)
        stack.scale(limbThick * 1.12f, 0.12f, limbThick * 1.85f)
        if (isPolice) {
            setMat(0.07f, 0.07f, 0.09f, spec = 0.65f, shiny = 48f)
        } else {
            setMat(0.20f, 0.16f, 0.14f, spec = 0.35f, shiny = 26f)
        }
        submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
        stack.pop()

        stack.pop()
    }

    private fun drawArm(
        submitter: DrawSubmitter,
        side: Float,
        shoulderX: Float,
        limbThick: Float,
        armSwingDeg: Float,
        elbowBendDeg: Float,
        shirtR: Float, shirtG: Float, shirtB: Float,
        skinR: Float, skinG: Float, skinB: Float,
        shortSleeve: Boolean
    ) {
        stack.push()
        stack.translate(shoulderX, 0.46f, 0f)
        stack.rotateDeg(side * 6f, 0f, 0f, 1f)
        stack.rotateDeg(armSwingDeg, 1f, 0f, 0f)

        // Upper Arm
        stack.push()
        stack.translate(0f, -0.14f, 0f)
        stack.scale(limbThick * 0.90f, 0.28f, limbThick * 0.90f)
        if (shortSleeve) {
            // Sleeve top half + skin lower half simulated cleanly with sleeve cuff
            setMat(shirtR, shirtG, shirtB, spec = 0.18f, shiny = 16f)
        } else {
            setMat(shirtR, shirtG, shirtB, spec = 0.18f, shiny = 16f)
        }
        submitter.drawMesh(meshes.capsule, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Elbow pivot
        stack.translate(0f, -0.28f, 0f)
        stack.rotateDeg(elbowBendDeg, 1f, 0f, 0f)

        // Forearm
        stack.push()
        stack.translate(0f, -0.13f, 0f)
        stack.scale(limbThick * 0.78f, 0.26f, limbThick * 0.78f)
        if (shortSleeve) {
            setMat(skinR, skinG, skinB, spec = 0.25f, shiny = 24f)
        } else {
            setMat(shirtR, shirtG, shirtB, spec = 0.18f, shiny = 16f)
        }
        submitter.drawMesh(meshes.capsule, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Hand
        stack.push()
        stack.translate(0f, -0.28f, 0.01f)
        stack.scale(limbThick * 0.84f, limbThick * 0.92f, limbThick * 0.84f)
        setMat(skinR, skinG, skinB, spec = 0.25f, shiny = 24f)
        submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
        stack.pop()

        stack.pop()
    }

    private fun drawDetailedHead(
        submitter: DrawSubmitter,
        role: CharacterRole,
        headScale: Float,
        skinR: Float, skinG: Float, skinB: Float,
        eyeOpenY: Float,
        mouthOpen: Float,
        isAlert: Boolean
    ) {
        val isMotu = role == CharacterRole.MOTU
        val isPatlu = role == CharacterRole.PATLU
        val isPolice = isMotu || isPatlu
        val isCriminal = role == CharacterRole.CRIMINAL

        // Cranium
        stack.push()
        stack.scale(headScale * (if (isMotu) 1.06f else 0.95f), headScale * (if (isMotu) 0.98f else 1.08f), headScale)
        setMat(skinR, skinG, skinB, spec = 0.24f, shiny = 24f, rim = 0.22f)
        submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Ears
        for (side in -1..1 step 2) {
            stack.push()
            stack.translate(side * headScale * 0.48f, -headScale * 0.02f, 0f)
            stack.scale(headScale * 0.14f, headScale * 0.22f, headScale * 0.12f)
            setMat(skinR * 0.94f, skinG * 0.90f, skinB * 0.90f, spec = 0.20f, shiny = 18f)
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            stack.pop()
        }

        // Cheeks (subtle warm subsurface glow)
        for (side in -1..1 step 2) {
            stack.push()
            stack.translate(side * headScale * 0.22f, -headScale * 0.09f, headScale * 0.39f)
            stack.scale(headScale * 0.16f, headScale * 0.11f, headScale * 0.08f)
            setMat(
                (skinR * 1.12f).coerceAtMost(1f),
                skinG * 0.96f,
                skinB * 0.93f,
                spec = 0.28f, shiny = 28f
            )
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            stack.pop()
        }

        // 3D Nose
        stack.push()
        stack.translate(0f, -headScale * 0.02f, headScale * 0.49f)
        stack.scale(
            headScale * (if (isMotu) 0.20f else 0.13f),
            headScale * (if (isMotu) 0.16f else 0.18f),
            headScale * 0.16f
        )
        setMat(skinR * 0.96f, skinG * 0.92f, skinB * 0.90f, spec = 0.28f, shiny = 26f)
        submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Expressive Eyes (Sclera + Iris + Glossy Highlight) + Animated Eyebrows
        val eyeZ = headScale * 0.43f
        val eyeY = headScale * 0.08f
        val eyeSpacing = headScale * 0.17f
        for (side in -1..1 step 2) {
            // Sclera
            stack.push()
            stack.translate(side * eyeSpacing, eyeY, eyeZ)
            stack.scale(headScale * 0.13f, headScale * 0.14f * eyeOpenY, headScale * 0.08f)
            setMat(0.98f, 0.98f, 0.99f, spec = 0.5f, shiny = 40f)
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Dark Iris / Pupil
            if (eyeOpenY > 0.4f) {
                stack.push()
                stack.translate(side * eyeSpacing, eyeY, eyeZ + headScale * 0.035f)
                stack.scale(headScale * 0.072f, headScale * 0.085f * eyeOpenY, headScale * 0.04f)
                setMat(0.08f, 0.09f, 0.12f, spec = 0.9f, shiny = 80f)
                submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
                stack.pop()
            }

            // Expressive Eyebrow
            stack.push()
            stack.translate(side * eyeSpacing, eyeY + headScale * 0.11f, eyeZ + headScale * 0.02f)
            val browTilt = if (isAlert || isCriminal) -side * 14f else side * 5f
            stack.rotateDeg(browTilt, 0f, 0f, 1f)
            stack.scale(headScale * 0.16f, headScale * 0.036f, headScale * 0.04f)
            setMat(0.06f, 0.06f, 0.07f, spec = 0.2f, shiny = 14f)
            submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
            stack.pop()
        }

        // Animated Mouth / Lips
        stack.push()
        stack.translate(0f, -headScale * 0.22f, headScale * 0.42f)
        stack.scale(headScale * 0.18f, headScale * (0.04f + mouthOpen * 0.06f), headScale * 0.07f)
        setMat(0.46f, 0.16f, 0.18f, spec = 0.25f, shiny = 22f)
        submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Signature Motu thick curved mustache
        if (isMotu) {
            for (side in -1..1 step 2) {
                stack.push()
                stack.translate(side * headScale * 0.10f, -headScale * 0.12f, headScale * 0.47f)
                stack.rotateDeg(-side * 14f, 0f, 0f, 1f)
                stack.scale(headScale * 0.20f, headScale * 0.085f, headScale * 0.08f)
                setMat(0.05f, 0.05f, 0.06f, spec = 0.32f, shiny = 24f)
                submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
                stack.pop()
            }
        }

        // Signature Patlu round 3D glasses (metallic rims + semi-gloss lenses + bridge)
        if (isPatlu) {
            for (side in -1..1 step 2) {
                stack.push()
                stack.translate(side * eyeSpacing, eyeY, eyeZ + headScale * 0.065f)
                stack.scale(headScale * 0.19f, headScale * 0.19f, headScale * 0.025f)
                setMat(0.14f, 0.16f, 0.22f, spec = 0.95f, shiny = 80f)
                submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
                stack.pop()

                // Inner subtle sky-tinted lens sheen
                stack.push()
                stack.translate(side * eyeSpacing, eyeY, eyeZ + headScale * 0.068f)
                stack.scale(headScale * 0.15f, headScale * 0.15f, headScale * 0.028f)
                setMat(0.75f, 0.90f, 1.0f, a = 0.28f, spec = 0.95f, shiny = 90f)
                submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
                stack.pop()
            }
            // Glasses bridge
            stack.push()
            stack.translate(0f, eyeY + headScale * 0.02f, eyeZ + headScale * 0.068f)
            stack.scale(headScale * 0.14f, headScale * 0.024f, headScale * 0.024f)
            setMat(0.14f, 0.16f, 0.22f, spec = 0.95f, shiny = 80f)
            submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
            stack.pop()
        }

        // Hair base
        stack.push()
        stack.translate(0f, headScale * 0.14f, -headScale * 0.06f)
        stack.scale(headScale * 1.04f, headScale * 0.78f, headScale * 1.02f)
        setMat(0.07f, 0.06f, 0.07f, spec = 0.26f, shiny = 20f)
        submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
        stack.pop()

        // Peaked Police Cap (for Motu & Patlu) or Criminal Cap
        if (isPolice) {
            // Cap crown
            stack.push()
            stack.translate(0f, headScale * 0.42f, -headScale * 0.02f)
            stack.scale(headScale * 1.12f, headScale * 0.24f, headScale * 1.12f)
            setMat(0.08f, 0.15f, 0.34f, spec = 0.32f, shiny = 28f)
            submitter.drawMesh(meshes.coneTaper, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Cap dark band
            stack.push()
            stack.translate(0f, headScale * 0.32f, -headScale * 0.01f)
            stack.scale(headScale * 1.02f, headScale * 0.10f, headScale * 1.02f)
            setMat(0.04f, 0.05f, 0.08f, spec = 0.5f, shiny = 40f)
            submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Glossy black front visor
            stack.push()
            stack.translate(0f, headScale * 0.27f, headScale * 0.42f)
            stack.rotateDeg(12f, 1f, 0f, 0f)
            stack.scale(headScale * 0.78f, headScale * 0.05f, headScale * 0.42f)
            setMat(0.03f, 0.04f, 0.06f, spec = 0.90f, shiny = 72f)
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Gold Cap Crest Emblem
            stack.push()
            stack.translate(0f, headScale * 0.41f, headScale * 0.50f)
            stack.scale(headScale * 0.18f, headScale * 0.18f, headScale * 0.05f)
            setMat(1.0f, 0.80f, 0.18f, spec = 0.95f, shiny = 80f)
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            stack.pop()
        } else if (isCriminal) {
            // Criminal dark beanie
            stack.push()
            stack.translate(0f, headScale * 0.34f, -headScale * 0.02f)
            stack.scale(headScale * 1.04f, headScale * 0.34f, headScale * 1.04f)
            setMat(0.14f, 0.14f, 0.16f, spec = 0.15f, shiny = 12f)
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            stack.pop()
        }
    }

    private fun setMat(
        r: Float, g: Float, b: Float,
        a: Float = 1f,
        spec: Float = 0.25f,
        shiny: Float = 24f,
        rim: Float = 0.18f
    ) {
        mat.r = r; mat.g = g; mat.b = b; mat.a = a
        mat.emissiveR = 0f; mat.emissiveG = 0f; mat.emissiveB = 0f; mat.emissiveStrength = 0f
        mat.specular = spec; mat.shininess = shiny; mat.rim = rim
        mat.uvScaleX = 1f; mat.uvScaleY = 1f
        mat.nightWindowBoost = 0f
    }
}
