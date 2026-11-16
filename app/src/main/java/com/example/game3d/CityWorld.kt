package com.example.game3d

import com.example.data.DistrictId
import kotlin.math.abs

enum class BuildingStyle {
    PRECINCT,
    GLASS_TOWER,
    BAZAAR_SHOP,
    CIVIC_HALL
}

data class BuildingBlock(
    val x: Float,
    val z: Float,
    val w: Float,
    val h: Float,
    val d: Float,
    val style: BuildingStyle,
    val tintR: Float = 1f,
    val tintG: Float = 1f,
    val tintB: Float = 1f
)

data class TreeProp(val x: Float, val z: Float, val scale: Float)
data class LampProp(val x: Float, val z: Float, val armYawDeg: Float)
data class BenchProp(val x: Float, val z: Float, val yawDeg: Float)

class CityWorld {
    val buildings = ArrayList<BuildingBlock>()
    val colliders = ArrayList<BoxCollider>()
    val trees = ArrayList<TreeProp>()
    val lamps = ArrayList<LampProp>()
    val benches = ArrayList<BenchProp>()

    // Road coordinates (centerlines of 14m wide roads)
    val roadX = floatArrayOf(-60f, 0f, 60f)
    val roadZ = floatArrayOf(-60f, 0f, 60f)

    // Traffic waypoints (clockwise loop around outer & inner avenues)
    val trafficLoops: List<List<Vec3>> = listOf(
        listOf(
            Vec3(-63.5f, 0f, -63.5f), Vec3(63.5f, 0f, -63.5f),
            Vec3(63.5f, 0f, 63.5f), Vec3(-63.5f, 0f, 63.5f)
        ),
        listOf(
            Vec3(-3.5f, 0f, -56.5f), Vec3(56.5f, 0f, -56.5f),
            Vec3(56.5f, 0f, 56.5f), Vec3(-3.5f, 0f, 56.5f)
        ),
        listOf(
            Vec3(3.5f, 0f, 56.5f), Vec3(-56.5f, 0f, 56.5f),
            Vec3(-56.5f, 0f, -56.5f), Vec3(3.5f, 0f, -56.5f)
        )
    )

    // Sidewalk pedestrian patrol loops
    val pedestrianLoops: List<List<Vec3>> = listOf(
        listOf(Vec3(-12f, 0f, -12f), Vec3(-48f, 0f, -12f), Vec3(-48f, 0f, -48f), Vec3(-12f, 0f, -48f)),
        listOf(Vec3(12f, 0f, -12f), Vec3(48f, 0f, -12f), Vec3(48f, 0f, -48f), Vec3(12f, 0f, -48f)),
        listOf(Vec3(-12f, 0f, 12f), Vec3(-48f, 0f, 12f), Vec3(-48f, 0f, 48f), Vec3(-12f, 0f, 48f)),
        listOf(Vec3(12f, 0f, 12f), Vec3(48f, 0f, 12f), Vec3(48f, 0f, 48f), Vec3(12f, 0f, 48f))
    )

    init {
        buildCity()
    }

    fun getDistrictAt(x: Float, z: Float): DistrictId {
        return when {
            x <= 0f && z <= 0f -> DistrictId.PRECINCT
            x > 0f && z <= 0f -> DistrictId.BAZAAR
            x <= 0f && z > 0f -> DistrictId.PARK
            else -> DistrictId.DOWNTOWN
        }
    }

    private fun buildCity() {
        // 1. Central Precinct (North-West block: x in [-50..-10], z in [-50..-10])
        addBuilding(-30f, -34f, 22f, 11f, 16f, BuildingStyle.PRECINCT)
        // Precinct wing & comms tower
        addBuilding(-42f, -34f, 8f, 16f, 12f, BuildingStyle.CIVIC_HALL, 0.85f, 0.90f, 0.98f)
        addBuilding(-16f, -42f, 10f, 9f, 10f, BuildingStyle.CIVIC_HALL, 0.88f, 0.92f, 0.98f)

        // 2. Bazaar Market (North-East block: x in [10..50], z in [-50..-10])
        addBuilding(22f, -22f, 13f, 9.5f, 12f, BuildingStyle.BAZAAR_SHOP, 1.0f, 0.94f, 0.88f)
        addBuilding(40f, -22f, 13f, 11f, 12f, BuildingStyle.BAZAAR_SHOP, 0.95f, 0.92f, 0.88f)
        addBuilding(22f, -41f, 14f, 12f, 12f, BuildingStyle.BAZAAR_SHOP, 0.98f, 0.90f, 0.86f)
        addBuilding(41f, -41f, 12f, 14f, 13f, BuildingStyle.GLASS_TOWER, 0.90f, 0.92f, 1.0f)

        // 3. Emerald Park (South-West block: x in [-50..-10], z in [10..50])
        // Central stone fountain obstacle collider
        colliders.add(BoxCollider(-34.2f, -25.8f, 25.8f, 34.2f, 3.5f))
        // Park canopy trees & benches
        val parkTreeCoords = arrayOf(
            floatArrayOf(-15f, 15f, 1.15f), floatArrayOf(-45f, 15f, 1.25f),
            floatArrayOf(-15f, 45f, 1.20f), floatArrayOf(-45f, 45f, 1.10f),
            floatArrayOf(-20f, 28f, 1.05f), floatArrayOf(-40f, 32f, 1.15f),
            floatArrayOf(-30f, 15f, 1.00f), floatArrayOf(-30f, 45f, 1.10f)
        )
        for (t in parkTreeCoords) {
            trees.add(TreeProp(t[0], t[1], t[2]))
            colliders.add(BoxCollider(t[0] - 0.6f, t[0] + 0.6f, t[1] - 0.6f, t[1] + 0.6f, 4f))
        }
        benches.add(BenchProp(-22f, 30f, 90f))
        benches.add(BenchProp(-38f, 30f, 270f))
        benches.add(BenchProp(-30f, 22f, 0f))
        benches.add(BenchProp(-30f, 38f, 180f))

        // 4. Downtown Skyline (South-East block: x in [10..50], z in [10..50])
        addBuilding(21f, 22f, 14f, 26f, 14f, BuildingStyle.GLASS_TOWER, 0.88f, 0.95f, 1.0f)
        addBuilding(41f, 21f, 14f, 34f, 14f, BuildingStyle.GLASS_TOWER, 0.92f, 0.96f, 1.0f)
        addBuilding(22f, 42f, 15f, 22f, 13f, BuildingStyle.GLASS_TOWER, 0.85f, 0.92f, 0.98f)
        addBuilding(42f, 42f, 13f, 29f, 14f, BuildingStyle.BAZAAR_SHOP, 0.95f, 0.95f, 0.98f)

        // Perimeter skyline buildings outside outer avenues (x/z +-82) to frame the city horizon!
        val outerOffsets = floatArrayOf(-82f, 82f)
        val alongOffsets = floatArrayOf(-45f, -15f, 15f, 45f)
        for (ox in outerOffsets) {
            for ((idx, az) in alongOffsets.withIndex()) {
                val h = 16f + (idx % 3) * 7f
                addBuilding(ox, az, 16f, h, 16f, if (idx % 2 == 0) BuildingStyle.GLASS_TOWER else BuildingStyle.BAZAAR_SHOP)
                addBuilding(az, ox, 16f, h + 3f, 16f, if (idx % 2 == 1) BuildingStyle.GLASS_TOWER else BuildingStyle.CIVIC_HALL)
            }
        }
        // Corner towers
        for (cx in outerOffsets) {
            for (cz in outerOffsets) {
                addBuilding(cx, cz, 18f, 28f, 18f, BuildingStyle.GLASS_TOWER)
            }
        }

        // Street trees along main boulevards
        val boulevardSpots = floatArrayOf(-44f, -18f, 18f, 44f)
        for (s in boulevardSpots) {
            trees.add(TreeProp(s, -9.2f, 0.85f))
            trees.add(TreeProp(s, 9.2f, 0.85f))
        }

        // Street lamps along roads
        val lampSpots = floatArrayOf(-46f, -24f, 24f, 46f)
        for (s in lampSpots) {
            lamps.add(LampProp(s, -7.8f, 0f))
            lamps.add(LampProp(s, 7.8f, 180f))
            lamps.add(LampProp(-7.8f, s, 90f))
            lamps.add(LampProp(7.8f, s, 270f))
        }
    }

    private fun addBuilding(
        x: Float,
        z: Float,
        w: Float,
        h: Float,
        d: Float,
        style: BuildingStyle,
        r: Float = 1f,
        g: Float = 1f,
        b: Float = 1f
    ) {
        buildings.add(BuildingBlock(x, z, w, h, d, style, r, g, b))
        colliders.add(
            BoxCollider(
                minX = x - w * 0.5f,
                maxX = x + w * 0.5f,
                minZ = z - d * 0.5f,
                maxZ = z + d * 0.5f,
                maxY = h
            )
        )
    }

    fun renderWorld(
        submitter: DrawSubmitter,
        meshes: SharedMeshes,
        textures: TextureSet,
        camX: Float,
        camZ: Float,
        nightFactor: Float,
        shadowPass: Boolean
    ) {
        val stack = MatrixStack(8)
        val mat = MaterialUniforms()

        // Ground base plane (grass/earth)
        if (!shadowPass) {
            stack.reset()
            stack.translate(0f, -0.02f, 0f)
            stack.scale(240f, 1f, 240f)
            mat.setSolid(0.85f, 0.90f, 0.85f, uvX = 48f, uvY = 48f, spec = 0.05f, shiny = 8f)
            submitter.drawMesh(meshes.groundQuad, stack.current, textures.grassTex, mat)

            // Raised District Sidewalk Blocks
            val blockCenters = arrayOf(
                floatArrayOf(-30f, -30f, 0f), // Precinct
                floatArrayOf(30f, -30f, 1f),  // Bazaar
                floatArrayOf(-30f, 30f, 2f),  // Park
                floatArrayOf(30f, 30f, 3f)    // Downtown
            )
            for (bc in blockCenters) {
                stack.reset()
                stack.translate(bc[0], 0.07f, bc[1])
                stack.scale(46f, 0.14f, 46f)
                val tex = when (bc[2].toInt()) {
                    2 -> textures.grassTex
                    0 -> textures.plazaTex
                    else -> textures.sidewalkTex
                }
                mat.setSolid(0.95f, 0.95f, 0.96f, uvX = 10f, uvY = 10f, spec = 0.14f, shiny = 16f)
                submitter.drawMesh(meshes.box, stack.current, tex, mat)
            }

            // Precinct Helipad / Courtyard Circle
            stack.reset()
            stack.translate(-30f, 0.15f, -18f)
            stack.scale(8.5f, 0.04f, 8.5f)
            mat.setSolid(0.18f, 0.34f, 0.66f, spec = 0.3f, shiny = 24f)
            submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)

            // Emerald Park Central Fountain Basin & Water
            stack.reset()
            stack.translate(-30f, 0.35f, 30f)
            stack.scale(7.6f, 0.55f, 7.6f)
            mat.setSolid(0.82f, 0.80f, 0.76f, spec = 0.25f, shiny = 20f)
            submitter.drawMesh(meshes.cylinder, stack.current, textures.plazaTex, mat)

            stack.reset()
            stack.translate(-30f, 0.58f, 30f)
            stack.scale(6.4f, 0.15f, 6.4f)
            mat.setSolid(0.22f, 0.66f, 0.90f, spec = 0.95f, shiny = 90f)
            submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)

            stack.reset()
            stack.translate(-30f, 1.45f, 30f)
            stack.scale(1.4f, 1.8f, 1.4f)
            mat.setSolid(0.86f, 0.84f, 0.80f, spec = 0.3f, shiny = 24f)
            submitter.drawMesh(meshes.coneTaper, stack.current, textures.whiteTex, mat)

            // Asphalt Roads (3 North-South + 3 East-West avenues)
            for (rx in roadX) {
                stack.reset()
                stack.translate(rx, 0.01f, 0f)
                stack.scale(14f, 1f, 220f)
                mat.setSolid(1f, 1f, 1f, uvX = 1f, uvY = 24f, spec = 0.35f + nightFactor * 0.35f, shiny = 36f)
                submitter.drawMesh(meshes.groundQuad, stack.current, textures.asphaltTex, mat)
            }
            for (rz in roadZ) {
                stack.reset()
                stack.translate(0f, 0.015f, rz)
                stack.rotateDeg(90f, 0f, 1f, 0f)
                stack.scale(14f, 1f, 220f)
                mat.setSolid(1f, 1f, 1f, uvX = 1f, uvY = 24f, spec = 0.35f + nightFactor * 0.35f, shiny = 36f)
                submitter.drawMesh(meshes.groundQuad, stack.current, textures.asphaltTex, mat)
            }
        }

        // Buildings with distance culling
        val cullDistSq = 155f * 155f
        for (i in buildings.indices) {
            val b = buildings[i]
            val dx = b.x - camX
            val dz = b.z - camZ
            if (dx * dx + dz * dz > cullDistSq) continue

            stack.reset()
            stack.translate(b.x, b.h * 0.5f, b.z)
            stack.scale(b.w, b.h, b.d)

            val tex = when (b.style) {
                BuildingStyle.GLASS_TOWER -> textures.buildingGlassTex
                BuildingStyle.BAZAAR_SHOP -> textures.buildingShopTex
                BuildingStyle.PRECINCT -> textures.precinctWallTex
                BuildingStyle.CIVIC_HALL -> textures.buildingGlassTex
            }
            val uvX = (b.w / 8f).coerceAtLeast(1f)
            val uvY = (b.h / 10f).coerceAtLeast(1f)
            val isTower = b.style == BuildingStyle.GLASS_TOWER
            mat.setSolid(
                b.tintR, b.tintG, b.tintB,
                uvX = uvX, uvY = uvY,
                spec = if (isTower) 0.78f else 0.25f,
                shiny = if (isTower) 64f else 22f,
                nightWindowBoost = nightFactor * (if (b.style == BuildingStyle.BAZAAR_SHOP) 0.85f else 0.70f)
            )
            submitter.drawMesh(meshes.box, stack.current, tex, mat)

            // Roof rim & Precinct Blue/Gold Beacon Canopy
            if (!shadowPass) {
                stack.reset()
                stack.translate(b.x, b.h + 0.3f, b.z)
                stack.scale(b.w + 0.5f, 0.6f, b.d + 0.5f)
                mat.setSolid(0.18f, 0.22f, 0.28f, spec = 0.4f, shiny = 32f)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)

                if (b.style == BuildingStyle.PRECINCT) {
                    // Front police portico canopy
                    stack.reset()
                    stack.translate(b.x, 3.8f, b.z + b.d * 0.5f + 2.1f)
                    stack.scale(10f, 0.55f, 4.2f)
                    mat.setSolid(0.12f, 0.34f, 0.82f, spec = 0.6f, shiny = 48f, emR = 0.1f, emG = 0.4f, emB = 1f, emS = 0.25f + nightFactor * 0.5f)
                    submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)

                    // Rooftop glowing POLICE sign board
                    stack.reset()
                    stack.translate(b.x, b.h + 1.5f, b.z + b.d * 0.42f)
                    stack.scale(9.2f, 1.5f, 0.5f)
                    mat.setSolid(0.15f, 0.55f, 1.0f, emR = 0.18f, emG = 0.62f, emB = 1.0f, emS = 0.75f + nightFactor * 0.8f)
                    submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
                }
            }
        }

        // Trees (trunk + lush dual canopy spheres)
        val propCullSq = 115f * 115f
        for (i in trees.indices) {
            val t = trees[i]
            val dx = t.x - camX
            val dz = t.z - camZ
            if (dx * dx + dz * dz > propCullSq) continue

            stack.reset()
            stack.translate(t.x, 1.5f * t.scale, t.z)
            stack.scale(0.55f * t.scale, 3.0f * t.scale, 0.55f * t.scale)
            mat.setSolid(0.38f, 0.24f, 0.14f, spec = 0.08f, shiny = 8f)
            submitter.drawMesh(meshes.coneTaper, stack.current, textures.whiteTex, mat)

            stack.reset()
            stack.translate(t.x, 3.6f * t.scale, t.z)
            stack.scale(2.8f * t.scale, 2.5f * t.scale, 2.8f * t.scale)
            mat.setSolid(0.18f, 0.54f, 0.22f, spec = 0.12f, shiny = 12f)
            submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)

            if (!shadowPass) {
                stack.reset()
                stack.translate(t.x + 0.5f * t.scale, 4.4f * t.scale, t.z - 0.3f * t.scale)
                stack.scale(2.0f * t.scale, 1.8f * t.scale, 2.0f * t.scale)
                mat.setSolid(0.24f, 0.64f, 0.26f, spec = 0.12f, shiny = 12f)
                submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            }
        }

        // Park benches
        if (!shadowPass) {
            for (b in benches) {
                if (abs(b.x - camX) > 100f || abs(b.z - camZ) > 100f) continue
                stack.reset()
                stack.translate(b.x, 0.45f, b.z)
                stack.rotateDeg(b.yawDeg, 0f, 1f, 0f)
                stack.scale(2.2f, 0.45f, 0.75f)
                mat.setSolid(0.52f, 0.32f, 0.18f, spec = 0.25f, shiny = 20f)
                submitter.drawMesh(meshes.box, stack.current, textures.whiteTex, mat)
            }
        }

        // Streetlamps (glowing warm globes at night)
        for (l in lamps) {
            val dx = l.x - camX
            val dz = l.z - camZ
            if (dx * dx + dz * dz > propCullSq) continue

            stack.reset()
            stack.translate(l.x, 2.4f, l.z)
            stack.scale(0.18f, 4.8f, 0.18f)
            mat.setSolid(0.20f, 0.24f, 0.30f, spec = 0.7f, shiny = 50f)
            submitter.drawMesh(meshes.cylinder, stack.current, textures.whiteTex, mat)

            if (!shadowPass) {
                stack.reset()
                stack.translate(l.x, 4.95f, l.z)
                stack.scale(0.55f, 0.42f, 0.55f)
                val lampGlow = 0.15f + nightFactor * 1.85f
                mat.setSolid(1.0f, 0.90f, 0.64f, emR = 1.0f, emG = 0.84f, emB = 0.48f, emS = lampGlow)
                submitter.drawMesh(meshes.sphere, stack.current, textures.whiteTex, mat)
            }
        }
    }
}

private fun MaterialUniforms.setSolid(
    r: Float, g: Float, b: Float,
    uvX: Float = 1f, uvY: Float = 1f,
    spec: Float = 0.25f, shiny: Float = 24f,
    emR: Float = 0f, emG: Float = 0f, emB: Float = 0f, emS: Float = 0f,
    nightWindowBoost: Float = 0f
) {
    this.r = r; this.g = g; this.b = b; this.a = 1f
    this.uvScaleX = uvX; this.uvScaleY = uvY
    this.specular = spec; this.shininess = shiny; this.rim = 0.16f
    this.emissiveR = emR; this.emissiveG = emG; this.emissiveB = emB; this.emissiveStrength = emS
    this.nightWindowBoost = nightWindowBoost
}
