package com.example.game3d

import android.opengl.Matrix
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(nx: Float, ny: Float, nz: Float) {
        x = nx; y = ny; z = nz
    }

    fun distSqXZ(ox: Float, oz: Float): Float {
        val dx = x - ox
        val dz = z - oz
        return dx * dx + dz * dz
    }

    fun distXZ(ox: Float, oz: Float): Float = sqrt(distSqXZ(ox, oz))
}

data class BoxCollider(
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float,
    val maxY: Float = 10f
)

object Math3D {
    const val DEG_TO_RAD = (PI / 180.0).toFloat()
    const val RAD_TO_DEG = (180.0 / PI).toFloat()

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    fun smoothDamp(current: Float, target: Float, factor: Float, dt: Float): Float {
        val t = 1f - kotlin.math.exp((-factor * dt).toDouble()).toFloat()
        return current + (target - current) * t
    }

    fun normalizeAngle(angleRad: Float): Float {
        var a = angleRad
        while (a > PI) a -= (2.0 * PI).toFloat()
        while (a < -PI) a += (2.0 * PI).toFloat()
        return a
    }

    fun lerpAngle(current: Float, target: Float, t: Float): Float {
        val diff = normalizeAngle(target - current)
        return normalizeAngle(current + diff * t.coerceIn(0f, 1f))
    }

    fun yawFromVector(dx: Float, dz: Float): Float = atan2(dx, dz)

    fun pushOutCircleVsBoxes(
        pos: Vec3,
        radius: Float,
        colliders: List<BoxCollider>
    ): Boolean {
        var collided = false
        for (i in colliders.indices) {
            val b = colliders[i]
            val closestX = pos.x.coerceIn(b.minX, b.maxX)
            val closestZ = pos.z.coerceIn(b.minZ, b.maxZ)
            val dx = pos.x - closestX
            val dz = pos.z - closestZ
            val distSq = dx * dx + dz * dz
            if (distSq < radius * radius) {
                collided = true
                if (distSq > 0.00001f) {
                    val dist = sqrt(distSq)
                    val push = radius - dist
                    pos.x += (dx / dist) * push
                    pos.z += (dz / dist) * push
                } else {
                    // Center inside box: push along nearest edge
                    val left = pos.x - b.minX
                    val right = b.maxX - pos.x
                    val top = pos.z - b.minZ
                    val bottom = b.maxZ - pos.z
                    val m = minOf(left, right, top, bottom)
                    when (m) {
                        left -> pos.x = b.minX - radius
                        right -> pos.x = b.maxX + radius
                        top -> pos.z = b.minZ - radius
                        else -> pos.z = b.maxZ + radius
                    }
                }
            }
        }
        return collided
    }
}

class MatrixStack(private val maxDepth: Int = 24) {
    private val stack = Array(maxDepth) { FloatArray(16) }
    private var top = 0

    init {
        Matrix.setIdentityM(stack[0], 0)
    }

    val current: FloatArray
        get() = stack[top]

    fun reset() {
        top = 0
        Matrix.setIdentityM(stack[0], 0)
    }

    fun push() {
        if (top + 1 < maxDepth) {
            System.arraycopy(stack[top], 0, stack[top + 1], 0, 16)
            top++
        }
    }

    fun pop() {
        if (top > 0) top--
    }

    fun translate(x: Float, y: Float, z: Float) {
        Matrix.translateM(stack[top], 0, x, y, z)
    }

    fun rotateDeg(angleDeg: Float, ax: Float, ay: Float, az: Float) {
        if (angleDeg != 0f) {
            Matrix.rotateM(stack[top], 0, angleDeg, ax, ay, az)
        }
    }

    fun rotateYRad(rad: Float) {
        if (rad != 0f) {
            Matrix.rotateM(stack[top], 0, rad * Math3D.RAD_TO_DEG, 0f, 1f, 0f)
        }
    }

    fun rotateXRad(rad: Float) {
        if (rad != 0f) {
            Matrix.rotateM(stack[top], 0, rad * Math3D.RAD_TO_DEG, 1f, 0f, 0f)
        }
    }

    fun rotateZRad(rad: Float) {
        if (rad != 0f) {
            Matrix.rotateM(stack[top], 0, rad * Math3D.RAD_TO_DEG, 0f, 0f, 1f)
        }
    }

    fun scale(sx: Float, sy: Float, sz: Float) {
        Matrix.scaleM(stack[top], 0, sx, sy, sz)
    }
}

fun setPerspectiveProjection(m: FloatArray, fovDeg: Float, aspect: Float, near: Float, far: Float) {
    Matrix.perspectiveM(m, 0, fovDeg, aspect, near, far)
}

fun sinF(x: Float): Float = sin(x.toDouble()).toFloat()
fun cosF(x: Float): Float = cos(x.toDouble()).toFloat()
