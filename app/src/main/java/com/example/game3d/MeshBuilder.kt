package com.example.game3d

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GpuMesh(
    private val vaoId: Int,
    private val vboId: Int,
    private val eboId: Int,
    private val indexCount: Int
) {
    fun draw() {
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        val vaos = intArrayOf(vaoId)
        val bufs = intArrayOf(vboId, eboId)
        GLES30.glDeleteVertexArrays(1, vaos, 0)
        GLES30.glDeleteBuffers(2, bufs, 0)
    }
}

object MeshBuilder {
    // Vertex layout: pos(3), normal(3), uv(2) = 8 floats = 32 bytes
    private const val STRIDE_BYTES = 8 * 4

    fun upload(vertices: FloatArray, indices: IntArray): GpuMesh {
        val vao = IntArray(1)
        val bufs = IntArray(2)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(2, bufs, 0)

        GLES30.glBindVertexArray(vao[0])

        val vb = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vb.position(0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bufs[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)

        val ib = ByteBuffer.allocateDirect(indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(indices)
        ib.position(0)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, bufs[1])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, ib, GLES30.GL_STATIC_DRAW)

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, STRIDE_BYTES, 0)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, STRIDE_BYTES, 3 * 4)

        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, STRIDE_BYTES, 6 * 4)

        GLES30.glBindVertexArray(0)
        return GpuMesh(vao[0], bufs[0], bufs[1], indices.size)
    }

    fun createBox(uvRepeatX: Float = 1f, uvRepeatY: Float = 1f): GpuMesh {
        // Unit cube centered at origin (-0.5..0.5)
        val verts = floatArrayOf(
            // Front (+Z)
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0f, uvRepeatY,
            0.5f, -0.5f, 0.5f, 0f, 0f, 1f, uvRepeatX, uvRepeatY,
            0.5f, 0.5f, 0.5f, 0f, 0f, 1f, uvRepeatX, 0f,
            -0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 0f, 0f,
            // Back (-Z)
            0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 0f, uvRepeatY,
            -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, uvRepeatX, uvRepeatY,
            -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, uvRepeatX, 0f,
            0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 0f, 0f,
            // Left (-X)
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0f, uvRepeatY,
            -0.5f, -0.5f, 0.5f, -1f, 0f, 0f, uvRepeatX, uvRepeatY,
            -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, uvRepeatX, 0f,
            -0.5f, 0.5f, -0.5f, -1f, 0f, 0f, 0f, 0f,
            // Right (+X)
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0f, uvRepeatY,
            0.5f, -0.5f, -0.5f, 1f, 0f, 0f, uvRepeatX, uvRepeatY,
            0.5f, 0.5f, -0.5f, 1f, 0f, 0f, uvRepeatX, 0f,
            0.5f, 0.5f, 0.5f, 1f, 0f, 0f, 0f, 0f,
            // Top (+Y)
            -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0f, uvRepeatY,
            0.5f, 0.5f, 0.5f, 0f, 1f, 0f, uvRepeatX, uvRepeatY,
            0.5f, 0.5f, -0.5f, 0f, 1f, 0f, uvRepeatX, 0f,
            -0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 0f, 0f,
            // Bottom (-Y)
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0f, uvRepeatY,
            0.5f, -0.5f, -0.5f, 0f, -1f, 0f, uvRepeatX, uvRepeatY,
            0.5f, -0.5f, 0.5f, 0f, -1f, 0f, uvRepeatX, 0f,
            -0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 0f, 0f
        )
        val idx = IntArray(36)
        for (f in 0 until 6) {
            val base = f * 4
            val o = f * 6
            idx[o] = base
            idx[o + 1] = base + 1
            idx[o + 2] = base + 2
            idx[o + 3] = base
            idx[o + 4] = base + 2
            idx[o + 5] = base + 3
        }
        return upload(verts, idx)
    }

    fun createGroundQuad(uvScaleX: Float = 1f, uvScaleZ: Float = 1f): GpuMesh {
        // Horizontal quad on XZ plane from -0.5..0.5, Y=0
        val verts = floatArrayOf(
            -0.5f, 0f, 0.5f, 0f, 1f, 0f, 0f, uvScaleZ,
            0.5f, 0f, 0.5f, 0f, 1f, 0f, uvScaleX, uvScaleZ,
            0.5f, 0f, -0.5f, 0f, 1f, 0f, uvScaleX, 0f,
            -0.5f, 0f, -0.5f, 0f, 1f, 0f, 0f, 0f
        )
        val idx = intArrayOf(0, 1, 2, 0, 2, 3)
        return upload(verts, idx)
    }

    fun createSphere(rings: Int = 18, sectors: Int = 24): GpuMesh {
        val verts = ArrayList<Float>((rings + 1) * (sectors + 1) * 8)
        val idx = ArrayList<Int>(rings * sectors * 6)
        for (r in 0..rings) {
            val v = r.toFloat() / rings
            val phi = v * PI
            val y = cos(phi).toFloat() * 0.5f
            val sinPhi = sin(phi).toFloat()
            for (s in 0..sectors) {
                val u = s.toFloat() / sectors
                val theta = u * 2.0 * PI
                val x = cos(theta).toFloat() * sinPhi * 0.5f
                val z = sin(theta).toFloat() * sinPhi * 0.5f
                val nx = cos(theta).toFloat() * sinPhi
                val ny = cos(phi).toFloat()
                val nz = sin(theta).toFloat() * sinPhi
                verts.add(x); verts.add(y); verts.add(z)
                verts.add(nx); verts.add(ny); verts.add(nz)
                verts.add(u); verts.add(v)
            }
        }
        val stride = sectors + 1
        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                val a = r * stride + s
                val b = a + 1
                val c = (r + 1) * stride + s
                val d = c + 1
                idx.add(a); idx.add(c); idx.add(b)
                idx.add(b); idx.add(c); idx.add(d)
            }
        }
        return upload(verts.toFloatArray(), idx.toIntArray())
    }

    fun createTaperedCylinder(
        segments: Int = 20,
        rBottom: Float = 0.5f,
        rTop: Float = 0.5f
    ): GpuMesh {
        // Height 1.0 centered from y=-0.5 to y=+0.5
        val verts = ArrayList<Float>()
        val idx = ArrayList<Int>()
        val slopeY = rBottom - rTop
        val invLen = 1f / sqrt(1f + slopeY * slopeY)

        for (i in 0..segments) {
            val u = i.toFloat() / segments
            val ang = u * 2.0 * PI
            val cx = cos(ang).toFloat()
            val cz = sin(ang).toFloat()
            val nx = cx * invLen
            val ny = slopeY * invLen
            val nz = cz * invLen

            // Top ring
            verts.add(cx * rTop); verts.add(0.5f); verts.add(cz * rTop)
            verts.add(nx); verts.add(ny); verts.add(nz)
            verts.add(u); verts.add(0f)

            // Bottom ring
            verts.add(cx * rBottom); verts.add(-0.5f); verts.add(cz * rBottom)
            verts.add(nx); verts.add(ny); verts.add(nz)
            verts.add(u); verts.add(1f)
        }

        for (i in 0 until segments) {
            val top0 = i * 2
            val bot0 = top0 + 1
            val top1 = top0 + 2
            val bot1 = top0 + 3
            idx.add(top0); idx.add(bot0); idx.add(top1)
            idx.add(top1); idx.add(bot0); idx.add(bot1)
        }

        // Top cap
        val topCenterIdx = verts.size / 8
        verts.add(0f); verts.add(0.5f); verts.add(0f)
        verts.add(0f); verts.add(1f); verts.add(0f)
        verts.add(0.5f); verts.add(0.5f)
        val topRingStart = verts.size / 8
        for (i in 0..segments) {
            val ang = (i.toFloat() / segments) * 2.0 * PI
            val cx = cos(ang).toFloat()
            val cz = sin(ang).toFloat()
            verts.add(cx * rTop); verts.add(0.5f); verts.add(cz * rTop)
            verts.add(0f); verts.add(1f); verts.add(0f)
            verts.add(0.5f + cx * 0.5f); verts.add(0.5f + cz * 0.5f)
        }
        for (i in 0 until segments) {
            idx.add(topCenterIdx); idx.add(topRingStart + i + 1); idx.add(topRingStart + i)
        }

        // Bottom cap
        val botCenterIdx = verts.size / 8
        verts.add(0f); verts.add(-0.5f); verts.add(0f)
        verts.add(0f); verts.add(-1f); verts.add(0f)
        verts.add(0.5f); verts.add(0.5f)
        val botRingStart = verts.size / 8
        for (i in 0..segments) {
            val ang = (i.toFloat() / segments) * 2.0 * PI
            val cx = cos(ang).toFloat()
            val cz = sin(ang).toFloat()
            verts.add(cx * rBottom); verts.add(-0.5f); verts.add(cz * rBottom)
            verts.add(0f); verts.add(-1f); verts.add(0f)
            verts.add(0.5f + cx * 0.5f); verts.add(0.5f + cz * 0.5f)
        }
        for (i in 0 until segments) {
            idx.add(botCenterIdx); idx.add(botRingStart + i); idx.add(botRingStart + i + 1)
        }

        return upload(verts.toFloatArray(), idx.toIntArray())
    }

    fun createCapsule(segments: Int = 18, rings: Int = 10, bodyHeight: Float = 0.6f, radius: Float = 0.35f): GpuMesh {
        // Smooth capsule centered at origin, great for organic limbs & torso!
        val verts = ArrayList<Float>()
        val idx = ArrayList<Int>()
        val halfBody = bodyHeight * 0.5f

        for (r in 0..rings) {
            val t = r.toFloat() / rings
            val phi = t * PI
            val sy = cos(phi).toFloat()
            val sinPhi = sin(phi).toFloat()
            val yOffset = if (r <= rings / 2) halfBody else -halfBody
            val y = sy * radius + yOffset
            for (s in 0..segments) {
                val u = s.toFloat() / segments
                val theta = u * 2.0 * PI
                val cx = cos(theta).toFloat() * sinPhi
                val cz = sin(theta).toFloat() * sinPhi
                verts.add(cx * radius); verts.add(y); verts.add(cz * radius)
                verts.add(cx); verts.add(sy); verts.add(cz)
                verts.add(u); verts.add(t)
            }
        }
        val stride = segments + 1
        for (r in 0 until rings) {
            for (s in 0 until segments) {
                val a = r * stride + s
                val b = a + 1
                val c = (r + 1) * stride + s
                val d = c + 1
                idx.add(a); idx.add(c); idx.add(b)
                idx.add(b); idx.add(c); idx.add(d)
            }
        }
        return upload(verts.toFloatArray(), idx.toIntArray())
    }
}
