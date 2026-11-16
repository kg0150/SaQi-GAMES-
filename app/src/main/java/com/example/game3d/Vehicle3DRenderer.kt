package com.example.game3d

enum class VehicleType {
    POLICE_CRUISER,
    POLICE_SUV,
    CIVILIAN_SEDAN,
    CITY_TAXI,
    GETAWAY_CAR
}

data class VehicleEntity(
    val id: Int,
    val type: VehicleType,
    val pos: Vec3,
    var yawRad: Float,
    var speed: Float = 0f,
    var steerAngleRad: Float = 0f,
    var wheelSpinRad: Float = 0f,
    var sirenOn: Boolean = false,
    var occupiedByPlayer: Boolean = false,
    var occupiedRole: CharacterRole? = null,
    var waypointIndex: Int = 0
)

class Vehicle3DRenderer(
    private val meshes: SharedMeshes,
    private val textures: TextureSet,
    private val characterRenderer: Character3DRenderer
) {
    private val stack = MatrixStack(16)
    private val mat = MaterialUniforms()
    private val seatedPose = CharacterPose(animState = CharacterAnimState.DRIVING)

    fun renderVehicle(
        submitter: DrawSubmitter,
        v: VehicleEntity,
        timeSec: Float,
        nightFactor: Float,
        transparentPass: Boolean
    ) {
        val isPolice = v.type == VehicleType.POLICE_CRUISER || v.type == VehicleType.POLICE_SUV
        val isSuv = v.type == VehicleType.POLICE_SUV
        val bodyW = if (isSuv) 2.15f else 1.96f
        val bodyL = if (isSuv) 4.45f else 4.25f
        val bodyH = if (isSuv) 0.78f else 0.64f
        val cabH = if (isSuv) 0.68f else 0.56f

        val (bodyR, bodyG, bodyB) = when (v.type) {
            VehicleType.POLICE_CRUISER -> Triple(0.10f, 0.20f, 0.45f)
            VehicleType.POLICE_SUV -> Triple(0.08f, 0.14f, 0.30f)
            VehicleType.CITY_TAXI -> Triple(0.95f, 0.74f, 0.12f)
            VehicleType.GETAWAY_CAR -> Triple(0.74f, 0.12f, 0.16f)
            VehicleType.CIVILIAN_SEDAN -> when (v.id % 3) {
                0 -> Triple(0.78f, 0.82f, 0.86f)
                1 -> Triple(0.24f, 0.54f, 0.68f)
                else -> Triple(0.32f, 0.36f, 0.42f)
            }
        }

        if (!transparentPass) {
            stack.reset()
            stack.push()
            stack.translate(v.pos.x, v.pos.y, v.pos.z)
            stack.rotateYRad(v.yawRad)

            // 4 Wheels with steering on front axle and spinning
            val wheelPositions = arrayOf(
                floatArrayOf(-bodyW * 0.48f, 0.38f, bodyL * 0.31f, 1f),
                floatArrayOf(bodyW * 0.48f, 0.38f, bodyL * 0.31f, 1f),
                floatArrayOf(-bodyW * 0.48f, 0.38f, -bodyL * 0.31f, 0f),
                floatArrayOf(bodyW * 0.48f, 0.38f, -bodyL * 0.31f, 0f)
            )
            for (wp in wheelPositions) {
                stack.push()
                stack.translate(wp[0], wp[1], wp[2])
                if (wp[3] > 0.5f) {
                    stack.rotateYRad(v.steerAngleRad)
                }
                stack.rotateXRad(v.wheelSpinRad)
                stack.rotateDeg(90f, 0f, 0f, 1f)

                // Rubber tire
                stack.push()
                stack.scale(0.76f, 0.28f, 0.76f)
                setMat(0.08f, 0.08f, 0.09f, spec = 0.20f, shiny = 14f)
                submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
                stack.pop()

                // Silver alloy rim
                stack.push()
                stack.scale(0.45f, 0.30f, 0.45f)
                setMat(0.84f, 0.87f, 0.92f, spec = 0.92f, shiny = 75f)
                submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)
                stack.pop()

                stack.pop()
            }

            // Main sculpted lower body
            stack.push()
            stack.translate(0f, 0.36f + bodyH * 0.5f, 0f)
            stack.scale(bodyW, bodyH, bodyL)
            val tex = if (isPolice) textures.policeLiveryTex else textures.whiteTex
            setMat(
                if (isPolice) 0.95f else bodyR,
                if (isPolice) 0.97f else bodyG,
                if (isPolice) 1.0f else bodyB,
                spec = 0.85f, shiny = 68f, rim = 0.25f
            )
            submitter.drawMesh(meshes.box, stack.current, tex, mat)
            stack.pop()

            // Chrome / Dark Front & Rear Bumpers
            for (zSign in -1..1 step 2) {
                stack.push()
                stack.translate(0f, 0.34f, zSign * (bodyL * 0.50f + 0.04f))
                stack.scale(bodyW * 0.96f, 0.22f, 0.14f)
                setMat(0.14f, 0.16f, 0.20f, spec = 0.7f, shiny = 50f)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                stack.pop()
            }

            // Solid Cabin Roof & Pillars
            val cabTopY = 0.36f + bodyH + cabH
            stack.push()
            stack.translate(0f, cabTopY - 0.06f, -0.18f)
            stack.scale(bodyW * 0.84f, 0.12f, bodyL * (if (isSuv) 0.58f else 0.46f))
            setMat(
                if (isPolice) 0.96f else bodyR,
                if (isPolice) 0.98f else bodyG,
                if (isPolice) 1.0f else bodyB,
                spec = 0.85f, shiny = 68f
            )
            submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
            stack.pop()

            // Police Roof Lightbar (alternating bright red/blue strobes!)
            if (isPolice) {
                val strobe = ((timeSec * 8f).toInt() % 2 == 0)
                val barY = cabTopY + 0.06f
                // Left Red Pod
                stack.push()
                stack.translate(-0.34f, barY, -0.15f)
                stack.scale(0.54f, 0.12f, 0.24f)
                val redLit = if (v.sirenOn) (if (strobe) 2.2f else 0.35f) else 0.20f
                setMat(0.95f, 0.12f, 0.18f, spec = 0.9f, shiny = 72f, emR = 1f, emG = 0.12f, emB = 0.22f, emS = redLit)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                stack.pop()

                // Center Dark Mount
                stack.push()
                stack.translate(0f, barY, -0.15f)
                stack.scale(0.18f, 0.11f, 0.22f)
                setMat(0.12f, 0.14f, 0.18f, spec = 0.7f, shiny = 50f)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                stack.pop()

                // Right Blue Pod
                stack.push()
                stack.translate(0.34f, barY, -0.15f)
                stack.scale(0.54f, 0.12f, 0.24f)
                val blueLit = if (v.sirenOn) (if (!strobe) 2.2f else 0.35f) else 0.20f
                setMat(0.12f, 0.55f, 1.0f, spec = 0.9f, shiny = 72f, emR = 0.10f, emG = 0.60f, emB = 1.0f, emS = blueLit)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                stack.pop()
            } else if (v.type == VehicleType.CITY_TAXI) {
                stack.push()
                stack.translate(0f, cabTopY + 0.06f, -0.10f)
                stack.scale(0.46f, 0.12f, 0.20f)
                setMat(1.0f, 0.88f, 0.35f, emR = 1f, emG = 0.85f, emB = 0.3f, emS = 0.4f + nightFactor * 0.7f)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                stack.pop()
            }

            // Headlights (glow warmly, brighter at night)
            val headGlow = 0.35f + nightFactor * 1.4f
            for (side in -1..1 step 2) {
                stack.push()
                stack.translate(side * bodyW * 0.36f, 0.62f, bodyL * 0.50f + 0.01f)
                stack.scale(0.28f, 0.14f, 0.05f)
                setMat(1f, 0.96f, 0.84f, emR = 1f, emG = 0.95f, emB = 0.82f, emS = headGlow)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                stack.pop()

                // Taillights
                stack.push()
                stack.translate(side * bodyW * 0.36f, 0.64f, -bodyL * 0.50f - 0.01f)
                stack.scale(0.30f, 0.14f, 0.05f)
                setMat(0.95f, 0.14f, 0.18f, emR = 1f, emG = 0.12f, emB = 0.15f, emS = 0.55f + nightFactor * 0.7f)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                stack.pop()
            }

            stack.pop()

            // Render seated officer inside cabin if occupied!
            val role = v.occupiedRole
            if (role != null) {
                seatedPose.animTime = timeSec
                seatedPose.leanAngleRad = -v.steerAngleRad * 0.4f
                val sinY = sinF(v.yawRad)
                val cosY = cosF(v.yawRad)
                val localX = -0.35f
                val localZ = -0.05f
                val worldX = v.pos.x + localX * cosY + localZ * sinY
                val worldZ = v.pos.z - localX * sinY + localZ * cosY
                characterRenderer.renderCharacter(
                    submitter = submitter,
                    role = role,
                    x = worldX,
                    y = v.pos.y + 0.38f,
                    z = worldZ,
                    yawRad = v.yawRad,
                    pose = seatedPose
                )
            }
        } else {
            // Semi-transparent glossy tinted glass cabin pass
            stack.reset()
            stack.push()
            stack.translate(v.pos.x, v.pos.y, v.pos.z)
            stack.rotateYRad(v.yawRad)
            stack.translate(0f, 0.36f + bodyH + cabH * 0.45f, -0.18f)
            stack.scale(bodyW * 0.82f, cabH * 0.86f, bodyL * (if (isSuv) 0.56f else 0.45f))
            setMat(0.18f, 0.32f, 0.48f, a = 0.42f, spec = 0.98f, shiny = 96f, rim = 0.35f)
            submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
            stack.pop()
        }
    }

    private fun setMat(
        r: Float, g: Float, b: Float,
        a: Float = 1f,
        spec: Float = 0.6f,
        shiny: Float = 48f,
        rim: Float = 0.22f,
        emR: Float = 0f, emG: Float = 0f, emB: Float = 0f, emS: Float = 0f
    ) {
        mat.r = r; mat.g = g; mat.b = b; mat.a = a
        mat.emissiveR = emR; mat.emissiveG = emG; mat.emissiveB = emB; mat.emissiveStrength = emS
        mat.specular = spec; mat.shininess = shiny; mat.rim = rim
        mat.uvScaleX = 1f; mat.uvScaleY = 1f
        mat.nightWindowBoost = 0f
    }
}
