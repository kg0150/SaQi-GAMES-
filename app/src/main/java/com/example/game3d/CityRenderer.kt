package com.example.game3d

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CityRenderer(
    private val simulation: GameSimulation
) : GLSurfaceView.Renderer, DrawSubmitter {

    private lateinit var meshes: SharedMeshes
    private lateinit var textures: TextureSet
    private lateinit var characterRenderer: Character3DRenderer
    private lateinit var vehicleRenderer: Vehicle3DRenderer

    private lateinit var shadowShader: ShaderProgram
    private lateinit var skyShader: ShaderProgram
    private lateinit var worldShader: ShaderProgram

    private var shadowFbo = 0
    private var shadowDepthTex = 0
    private val shadowSize = 1024

    private var viewportW = 1280
    private var viewportH = 720
    @Volatile var dynamicShadowsEnabled = true

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val viewProjMatrix = FloatArray(16)
    private val lightViewMatrix = FloatArray(16)
    private val lightProjMatrix = FloatArray(16)
    private val lightSpaceMatrix = FloatArray(16)
    private var lastFrameNs = 0L

    private var activeShaderIsShadow = false

    // Cached uniform locations for world shader
    private var uViewProj = 0
    private var uModel = 0
    private var uLightSpaceMatrix = 0
    private var uUvScale = 0
    private var uAlbedoTex = 0
    private var uShadowMap = 0
    private var uBaseColor = 0
    private var uEmissiveColor = 0
    private var uEmissiveStrength = 0
    private var uSpecularStrength = 0
    private var uShininess = 0
    private var uRimStrength = 0
    private var uNightWindowBoost = 0
    private var uCamPos = 0
    private var uSunDir = 0
    private var uSunColor = 0
    private var uSkyAmbient = 0
    private var uGroundAmbient = 0
    private var uFogColor = 0
    private var uFogDensity = 0
    private var uShadowEnabled = 0
    private var uPointLightCount = 0
    private var uPointLightPos = 0
    private var uPointLightColor = 0
    private var uPointLightRadius = 0

    // Shadow shader uniforms
    private var uShadowLightSpace = 0
    private var uShadowModel = 0

    private val pointPos = FloatArray(18)
    private val pointColor = FloatArray(18)
    private val pointRadius = FloatArray(6)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        meshes = SharedMeshes().apply { init() }
        textures = TextureSet().apply { initialize() }
        characterRenderer = Character3DRenderer(meshes, textures)
        vehicleRenderer = Vehicle3DRenderer(meshes, textures, characterRenderer)

        shadowShader = ShaderProgram(Shaders.SHADOW_VERTEX, Shaders.SHADOW_FRAGMENT)
        skyShader = ShaderProgram(Shaders.SKY_VERTEX, Shaders.SKY_FRAGMENT)
        worldShader = ShaderProgram(Shaders.WORLD_VERTEX, Shaders.WORLD_FRAGMENT)

        cacheUniforms()
        initShadowFbo()
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportW = width.coerceAtLeast(1)
        viewportH = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, viewportW, viewportH)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = now

        simulation.step(dt)

        // Compute dynamic sun/moon & atmospheric lighting from timeOfDayNormalized (0..1)
        val tod = simulation.timeOfDayNormalized
        val sunAngle = tod * 2f * PI.toFloat() - 0.35f
        val sunElev = sinF(sunAngle)
        val nightFactor = ((-sunElev + 0.12f) / 0.45f).coerceIn(0f, 1f)
        val sunsetFactor = (1f - kotlin.math.abs(sunElev) / 0.35f).coerceIn(0f, 1f)

        val dirX = cosF(sunAngle) * 0.65f
        val dirY = if (sunElev > 0.08f) sunElev else 0.42f // Moon takes over directional shadow at night
        val dirZ = 0.55f
        val invL = 1f / kotlin.math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        val lDirX = dirX * invL
        val lDirY = dirY * invL
        val lDirZ = dirZ * invL

        // Sky, Horizon, Sun, Ambient colors
        val zenithR = Math3D.lerp(0.14f, 0.03f, nightFactor) + sunsetFactor * 0.14f
        val zenithG = Math3D.lerp(0.42f, 0.05f, nightFactor)
        val zenithB = Math3D.lerp(0.84f, 0.14f, nightFactor)

        val horizonR = Math3D.lerp(0.62f, 0.07f, nightFactor) + sunsetFactor * 0.34f
        val horizonG = Math3D.lerp(0.76f, 0.10f, nightFactor) + sunsetFactor * 0.12f
        val horizonB = Math3D.lerp(0.92f, 0.22f, nightFactor)

        val sunR = Math3D.lerp(1.00f, 0.38f, nightFactor) + sunsetFactor * 0.12f
        val sunG = Math3D.lerp(0.96f, 0.46f, nightFactor) - sunsetFactor * 0.28f
        val sunB = Math3D.lerp(0.88f, 0.72f, nightFactor) - sunsetFactor * 0.45f

        val skyAmbR = Math3D.lerp(0.42f, 0.11f, nightFactor) + sunsetFactor * 0.10f
        val skyAmbG = Math3D.lerp(0.50f, 0.14f, nightFactor)
        val skyAmbB = Math3D.lerp(0.62f, 0.25f, nightFactor)

        val gndAmbR = Math3D.lerp(0.24f, 0.06f, nightFactor)
        val gndAmbG = Math3D.lerp(0.24f, 0.07f, nightFactor)
        val gndAmbB = Math3D.lerp(0.26f, 0.10f, nightFactor)

        // Update Camera matrices
        val aspect = viewportW.toFloat() / viewportH.toFloat()
        setPerspectiveProjection(projMatrix, simulation.camFov, aspect, 0.4f, 340f)
        Matrix.setLookAtM(
            viewMatrix, 0,
            simulation.camPos.x, simulation.camPos.y, simulation.camPos.z,
            simulation.camTarget.x, simulation.camTarget.y, simulation.camTarget.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // Light Space Matrix centered around player for crisp shadows
        val focusX = simulation.playerPos.x
        val focusZ = simulation.playerPos.z
        Matrix.setLookAtM(
            lightViewMatrix, 0,
            focusX + lDirX * 52f, lDirY * 52f, focusZ + lDirZ * 52f,
            focusX, 0f, focusZ,
            0f, 1f, 0f
        )
        Matrix.orthoM(lightProjMatrix, 0, -48f, 48f, -48f, 48f, 5f, 120f)
        Matrix.multiplyMM(lightSpaceMatrix, 0, lightProjMatrix, 0, lightViewMatrix, 0)

        // 1. SHADOW DEPTH PASS
        if (dynamicShadowsEnabled) {
            activeShaderIsShadow = true
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
            GLES30.glViewport(0, 0, shadowSize, shadowSize)
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
            GLES30.glCullFace(GLES30.GL_FRONT)
            shadowShader.use()
            GLES30.glUniformMatrix4fv(uShadowLightSpace, 1, false, lightSpaceMatrix, 0)

            renderSceneGeometry(nightFactor, shadowPass = true, transparentPass = false)

            GLES30.glCullFace(GLES30.GL_BACK)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }

        // 2. MAIN COLOR PASS
        activeShaderIsShadow = false
        GLES30.glViewport(0, 0, viewportW, viewportH)
        GLES30.glClearColor(horizonR, horizonG, horizonB, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Draw Sky Dome
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glCullFace(GLES30.GL_FRONT)
        skyShader.use()
        GLES30.glUniformMatrix4fv(skyShader.loc("uViewProj"), 1, false, viewProjMatrix, 0)
        GLES30.glUniform3f(skyShader.loc("uCamPos"), simulation.camPos.x, simulation.camPos.y, simulation.camPos.z)
        GLES30.glUniform3f(skyShader.loc("uZenithColor"), zenithR, zenithG, zenithB)
        GLES30.glUniform3f(skyShader.loc("uHorizonColor"), horizonR, horizonG, horizonB)
        GLES30.glUniform3f(skyShader.loc("uSunDir"), lDirX, lDirY, lDirZ)
        GLES30.glUniform3f(skyShader.loc("uSunColor"), sunR, sunG, sunB)
        GLES30.glUniform1f(skyShader.loc("uNightFactor"), nightFactor)
        GLES30.glUniform1f(skyShader.loc("uTimeSec"), simulation.worldTimeSec)
        meshes.box.draw()
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glDepthFunc(GLES30.GL_LESS)

        // Setup World Shader
        worldShader.use()
        GLES30.glUniformMatrix4fv(uViewProj, 1, false, viewProjMatrix, 0)
        GLES30.glUniformMatrix4fv(uLightSpaceMatrix, 1, false, lightSpaceMatrix, 0)
        GLES30.glUniform3f(uCamPos, simulation.camPos.x, simulation.camPos.y, simulation.camPos.z)
        GLES30.glUniform3f(uSunDir, lDirX, lDirY, lDirZ)
        GLES30.glUniform3f(uSunColor, sunR, sunG, sunB)
        GLES30.glUniform3f(uSkyAmbient, skyAmbR, skyAmbG, skyAmbB)
        GLES30.glUniform3f(uGroundAmbient, gndAmbR, gndAmbG, gndAmbB)
        GLES30.glUniform3f(uFogColor, horizonR, horizonG, horizonB)
        GLES30.glUniform1f(uFogDensity, 0.0046f + nightFactor * 0.0018f)
        GLES30.glUniform1f(uShadowEnabled, if (dynamicShadowsEnabled) 1f else 0f)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowDepthTex)
        GLES30.glUniform1i(uShadowMap, 1)
        GLES30.glUniform1i(uAlbedoTex, 0)

        uploadDynamicPointLights(nightFactor)

        // Opaque Geometry Pass
        renderSceneGeometry(nightFactor, shadowPass = false, transparentPass = false)

        // Transparent Glass & Beacon Pass
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        renderSceneGeometry(nightFactor, shadowPass = false, transparentPass = true)
        renderCheckpointBeacon()
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun renderSceneGeometry(nightFactor: Float, shadowPass: Boolean, transparentPass: Boolean) {
        if (!transparentPass) {
            simulation.world.renderWorld(
                submitter = this,
                meshes = meshes,
                textures = textures,
                camX = simulation.camPos.x,
                camZ = simulation.camPos.z,
                nightFactor = nightFactor,
                shadowPass = shadowPass
            )

            // Render Player Character if on foot
            if (simulation.drivenVehicleId == null) {
                characterRenderer.renderCharacter(
                    submitter = this,
                    role = simulation.activeRole(),
                    x = simulation.playerPos.x,
                    y = simulation.playerPos.y,
                    z = simulation.playerPos.z,
                    yawRad = simulation.playerYaw,
                    pose = simulation.playerPose
                )
            }

            // Render Partner Officer (Motu or Patlu)
            if (simulation.drivenVehicleId == null) {
                characterRenderer.renderCharacter(
                    submitter = this,
                    role = simulation.partnerRole(),
                    x = simulation.partnerPos.x,
                    y = simulation.partnerPos.y,
                    z = simulation.partnerPos.z,
                    yawRad = simulation.partnerYaw,
                    pose = simulation.partnerPose
                )
            }

            // Render Civilians & Criminals (with distance culling)
            val cx = simulation.camPos.x
            val cz = simulation.camPos.z
            for (c in simulation.civilians) {
                if (c.pos.distSqXZ(cx, cz) > 85f * 85f) continue
                characterRenderer.renderCharacter(
                    submitter = this,
                    role = c.role,
                    x = c.pos.x,
                    y = c.pos.y,
                    z = c.pos.z,
                    yawRad = c.yawRad,
                    pose = c.pose,
                    variantSeed = c.id
                )
            }
            for (cr in simulation.criminals) {
                if (cr.pos.distSqXZ(cx, cz) > 95f * 95f) continue
                characterRenderer.renderCharacter(
                    submitter = this,
                    role = cr.role,
                    x = cr.pos.x,
                    y = cr.pos.y,
                    z = cr.pos.z,
                    yawRad = cr.yawRad,
                    pose = cr.pose,
                    variantSeed = cr.id
                )
                if (!shadowPass && !cr.isApprehended && cr.isActiveTarget) {
                    renderTargetHalo(cr.pos.x, 2.35f, cr.pos.z, 1f, 0.22f, 0.32f)
                }
            }
        }

        // Vehicles (Opaque pass + Glass pass)
        for (v in simulation.vehicles) {
            if (v.pos.distSqXZ(simulation.camPos.x, simulation.camPos.z) > 120f * 120f) continue
            vehicleRenderer.renderVehicle(
                submitter = this,
                v = v,
                timeSec = simulation.worldTimeSec,
                nightFactor = nightFactor,
                transparentPass = transparentPass
            )
        }
    }

    private fun renderTargetHalo(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float) {
        val m = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        val bob = sinF(simulation.worldTimeSec * 4.5f) * 0.14f
        Matrix.translateM(m, 0, x, y + bob, z)
        Matrix.rotateM(m, 0, simulation.worldTimeSec * 90f, 0f, 1f, 0f)
        Matrix.scaleM(m, 0, 0.35f, 0.35f, 0.35f)
        val mat = MaterialUniforms(
            r = r, g = g, b = b, a = 1f,
            emissiveR = r, emissiveG = g, emissiveB = b, emissiveStrength = 1.6f
        )
        drawMesh(meshes.box, m, textures.whiteTex, mat)
    }

    private fun renderCheckpointBeacon() {
        val cp = simulation.checkpointPos ?: return
        if (!simulation.checkpointVisible) return
        val pulse = 1.0f + sinF(simulation.worldTimeSec * 4.2f) * 0.15f
        val m = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        Matrix.translateM(m, 0, cp.x, 0.08f, cp.z)
        Matrix.scaleM(m, 0, 3.6f * pulse, 0.18f, 3.6f * pulse)
        val mat = MaterialUniforms(
            r = 0.08f, g = 0.85f, b = 1.0f, a = 0.55f,
            emissiveR = 0.10f, emissiveG = 0.85f, emissiveB = 1.0f, emissiveStrength = 1.4f
        )
        drawMesh(meshes.cylinder, m, textures.whiteTex, mat)

        // Vertical translucent beam
        Matrix.setIdentityM(m, 0)
        Matrix.translateM(m, 0, cp.x, 4.5f, cp.z)
        Matrix.scaleM(m, 0, 0.7f * pulse, 9.0f, 0.7f * pulse)
        mat.a = 0.22f
        drawMesh(meshes.cylinder, m, textures.whiteTex, mat)
    }

    private fun uploadDynamicPointLights(nightFactor: Float) {
        var count = 0

        // 1. Police Siren Lightbar dynamic strobe light
        for (v in simulation.vehicles) {
            if (v.sirenOn && count < 6) {
                val strobe = ((simulation.worldTimeSec * 8f).toInt() % 2 == 0)
                pointPos[count * 3] = v.pos.x
                pointPos[count * 3 + 1] = v.pos.y + 2.1f
                pointPos[count * 3 + 2] = v.pos.z
                if (strobe) {
                    pointColor[count * 3] = 1.6f; pointColor[count * 3 + 1] = 0.18f; pointColor[count * 3 + 2] = 0.25f
                } else {
                    pointColor[count * 3] = 0.18f; pointColor[count * 3 + 1] = 0.65f; pointColor[count * 3 + 2] = 1.7f
                }
                pointRadius[count] = 18f
                count++
            }
        }

        // 2. Mission Checkpoint beacon glow
        simulation.checkpointPos?.takeIf { simulation.checkpointVisible && count < 6 }?.let { cp ->
            pointPos[count * 3] = cp.x
            pointPos[count * 3 + 1] = 2.2f
            pointPos[count * 3 + 2] = cp.z
            pointColor[count * 3] = 0.15f; pointColor[count * 3 + 1] = 0.95f; pointColor[count * 3 + 2] = 1.25f
            pointRadius[count] = 14f
            count++
        }

        // 3. Nearest street lamps when dusk/night
        if (nightFactor > 0.18f) {
            val px = simulation.playerPos.x
            val pz = simulation.playerPos.z
            val sorted = simulation.world.lamps
                .sortedBy { (it.x - px) * (it.x - px) + (it.z - pz) * (it.z - pz) }
            for (lamp in sorted) {
                if (count >= 6) break
                pointPos[count * 3] = lamp.x
                pointPos[count * 3 + 1] = 4.6f
                pointPos[count * 3 + 2] = lamp.z
                pointColor[count * 3] = 1.15f * nightFactor
                pointColor[count * 3 + 1] = 0.86f * nightFactor
                pointColor[count * 3 + 2] = 0.42f * nightFactor
                pointRadius[count] = 18f
                count++
            }
        }

        GLES30.glUniform1i(uPointLightCount, count)
        if (count > 0) {
            GLES30.glUniform3fv(uPointLightPos, count, pointPos, 0)
            GLES30.glUniform3fv(uPointLightColor, count, pointColor, 0)
            GLES30.glUniform1fv(uPointLightRadius, count, pointRadius, 0)
        }
    }

    override fun drawMesh(mesh: GpuMesh, modelMatrix: FloatArray, textureId: Int, mat: MaterialUniforms) {
        if (activeShaderIsShadow) {
            GLES30.glUniformMatrix4fv(uShadowModel, 1, false, modelMatrix, 0)
            mesh.draw()
            return
        }
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
        GLES30.glUniform2f(uUvScale, mat.uvScaleX, mat.uvScaleY)
        GLES30.glUniform4f(uBaseColor, mat.r, mat.g, mat.b, mat.a)
        GLES30.glUniform3f(uEmissiveColor, mat.emissiveR, mat.emissiveG, mat.emissiveB)
        GLES30.glUniform1f(uEmissiveStrength, mat.emissiveStrength)
        GLES30.glUniform1f(uSpecularStrength, mat.specular)
        GLES30.glUniform1f(uShininess, mat.shininess)
        GLES30.glUniform1f(uRimStrength, mat.rim)
        GLES30.glUniform1f(uNightWindowBoost, mat.nightWindowBoost)
        mesh.draw()
    }

    private fun cacheUniforms() {
        uShadowLightSpace = shadowShader.loc("uLightSpaceMatrix")
        uShadowModel = shadowShader.loc("uModel")

        uViewProj = worldShader.loc("uViewProj")
        uModel = worldShader.loc("uModel")
        uLightSpaceMatrix = worldShader.loc("uLightSpaceMatrix")
        uUvScale = worldShader.loc("uUvScale")
        uAlbedoTex = worldShader.loc("uAlbedoTex")
        uShadowMap = worldShader.loc("uShadowMap")
        uBaseColor = worldShader.loc("uBaseColor")
        uEmissiveColor = worldShader.loc("uEmissiveColor")
        uEmissiveStrength = worldShader.loc("uEmissiveStrength")
        uSpecularStrength = worldShader.loc("uSpecularStrength")
        uShininess = worldShader.loc("uShininess")
        uRimStrength = worldShader.loc("uRimStrength")
        uNightWindowBoost = worldShader.loc("uNightWindowBoost")
        uCamPos = worldShader.loc("uCamPos")
        uSunDir = worldShader.loc("uSunDir")
        uSunColor = worldShader.loc("uSunColor")
        uSkyAmbient = worldShader.loc("uSkyAmbient")
        uGroundAmbient = worldShader.loc("uGroundAmbient")
        uFogColor = worldShader.loc("uFogColor")
        uFogDensity = worldShader.loc("uFogDensity")
        uShadowEnabled = worldShader.loc("uShadowEnabled")
        uPointLightCount = worldShader.loc("uPointLightCount")
        uPointLightPos = worldShader.loc("uPointLightPos")
        uPointLightColor = worldShader.loc("uPointLightColor")
        uPointLightRadius = worldShader.loc("uPointLightRadius")
    }

    private fun initShadowFbo() {
        val fbos = IntArray(1)
        val texs = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, texs, 0)
        shadowFbo = fbos[0]
        shadowDepthTex = texs[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowDepthTex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
            shadowSize, shadowSize, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
            GLES30.GL_TEXTURE_2D, shadowDepthTex, 0
        )
        GLES30.glDrawBuffers(1, intArrayOf(GLES30.GL_NONE), 0)
        GLES30.glReadBuffer(GLES30.GL_NONE)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }
}

@SuppressLint("ViewConstructor")
class PatrolGLSurfaceView(
    context: Context,
    private val simulation: GameSimulation
) : GLSurfaceView(context) {

    val renderer: CityRenderer
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
        renderer = CityRenderer(simulation)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!simulation.isPlaying || simulation.isPaused) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y
                simulation.input.camOrbitDeltaYaw += dx
                simulation.input.camOrbitDeltaPitch += dy
            }
        }
        return true
    }
}
