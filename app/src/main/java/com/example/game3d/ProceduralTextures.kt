package com.example.game3d

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.random.Random

class TextureSet {
    var whiteTex = 0; private set
    var asphaltTex = 0; private set
    var sidewalkTex = 0; private set
    var grassTex = 0; private set
    var plazaTex = 0; private set
    var buildingGlassTex = 0; private set
    var buildingShopTex = 0; private set
    var precinctWallTex = 0; private set
    var policeLiveryTex = 0; private set

    fun initialize() {
        whiteTex = uploadBitmap(createWhiteBitmap(), repeat = true)
        asphaltTex = uploadBitmap(createAsphaltBitmap(), repeat = true)
        sidewalkTex = uploadBitmap(createSidewalkBitmap(), repeat = true)
        grassTex = uploadBitmap(createGrassBitmap(), repeat = true)
        plazaTex = uploadBitmap(createPlazaBitmap(), repeat = true)
        buildingGlassTex = uploadBitmap(createBuildingGlassBitmap(), repeat = true)
        buildingShopTex = uploadBitmap(createBuildingShopBitmap(), repeat = true)
        precinctWallTex = uploadBitmap(createPrecinctWallBitmap(), repeat = true)
        policeLiveryTex = uploadBitmap(createPoliceLiveryBitmap(), repeat = false)
    }

    fun release() {
        val ids = intArrayOf(
            whiteTex, asphaltTex, sidewalkTex, grassTex, plazaTex,
            buildingGlassTex, buildingShopTex, precinctWallTex, policeLiveryTex
        )
        GLES30.glDeleteTextures(ids.size, ids, 0)
    }

    private fun uploadBitmap(bmp: Bitmap, repeat: Boolean): Int {
        val tex = IntArray(1)
        GLES30.glGenTextures(1, tex, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
        val wrap = if (repeat) GLES30.GL_REPEAT else GLES30.GL_CLAMP_TO_EDGE
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        bmp.recycle()
        return tex[0]
    }

    private fun createWhiteBitmap(): Bitmap {
        return Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
    }

    private fun createAsphaltBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(42, 46, 54))
        val p = Paint()
        val rnd = Random(101)
        for (i in 0 until 4500) {
            val x = rnd.nextFloat() * size
            val y = rnd.nextFloat() * size
            val v = rnd.nextInt(32, 68)
            p.color = Color.rgb(v, v + 2, v + 6)
            c.drawRect(x, y, x + 2f, y + 2f, p)
        }
        // Subtle tire wear lanes
        p.color = Color.argb(30, 15, 18, 24)
        c.drawRect(44f, 0f, 82f, size.toFloat(), p)
        c.drawRect(174f, 0f, 212f, size.toFloat(), p)
        // Dashed yellow centerline
        p.color = Color.rgb(235, 190, 45)
        c.drawRect(124f, 16f, 132f, 96f, p)
        c.drawRect(124f, 144f, 132f, 224f, p)
        // Edge white lines
        p.color = Color.rgb(215, 220, 228)
        c.drawRect(8f, 0f, 14f, size.toFloat(), p)
        c.drawRect(242f, 0f, 248f, size.toFloat(), p)
        return bmp
    }

    private fun createSidewalkBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(154, 158, 166))
        val p = Paint()
        val tile = 64f
        val rnd = Random(202)
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                val shade = rnd.nextInt(142, 168)
                p.color = Color.rgb(shade, shade + 2, shade + 6)
                c.drawRect(x * tile + 2f, y * tile + 2f, (x + 1) * tile - 2f, (y + 1) * tile - 2f, p)
            }
        }
        p.color = Color.rgb(108, 114, 122)
        p.strokeWidth = 3f
        for (i in 0..4) {
            c.drawLine(i * tile, 0f, i * tile, size.toFloat(), p)
            c.drawLine(0f, i * tile, size.toFloat(), i * tile, p)
        }
        return bmp
    }

    private fun createGrassBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(54, 118, 56))
        val p = Paint()
        val rnd = Random(303)
        for (i in 0 until 3200) {
            val x = rnd.nextFloat() * size
            val y = rnd.nextFloat() * size
            p.color = Color.rgb(rnd.nextInt(40, 74), rnd.nextInt(100, 145), rnd.nextInt(42, 72))
            c.drawRect(x, y, x + 3f, y + 4f, p)
        }
        return bmp
    }

    private fun createPlazaBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(196, 184, 165))
        val p = Paint()
        val step = 32f
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val even = (x + y) % 2 == 0
                p.color = if (even) Color.rgb(206, 194, 174) else Color.rgb(176, 164, 146)
                c.drawRect(x * step + 1f, y * step + 1f, (x + 1) * step - 1f, (y + 1) * step - 1f, p)
            }
        }
        return bmp
    }

    private fun createBuildingGlassBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(45, 56, 72))
        val p = Paint()
        val cols = 4
        val rows = 6
        val cw = size / cols.toFloat()
        val rh = size / rows.toFloat()
        val rnd = Random(404)
        for (r in 0 until rows) {
            for (col in 0 until cols) {
                val warmLit = rnd.nextFloat() < 0.42f
                p.color = if (warmLit) {
                    Color.rgb(238, 212, 146)
                } else {
                    Color.rgb(88, 134, 178)
                }
                c.drawRect(col * cw + 6f, r * rh + 5f, (col + 1) * cw - 6f, (r + 1) * rh - 5f, p)
                // Glass specular diagonal highlight
                p.color = Color.argb(50, 255, 255, 255)
                c.drawRect(col * cw + 8f, r * rh + 7f, col * cw + 18f, (r + 1) * rh - 7f, p)
            }
            // Floor slab trim
            p.color = Color.rgb(34, 42, 56)
            c.drawRect(0f, r * rh, size.toFloat(), r * rh + 3f, p)
        }
        return bmp
    }

    private fun createBuildingShopBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(186, 152, 124))
        val p = Paint()
        // Upper stories windows
        for (r in 0 until 3) {
            for (col in 0 until 4) {
                p.color = if ((r + col) % 2 == 0) Color.rgb(244, 216, 142) else Color.rgb(72, 92, 114)
                c.drawRect(col * 64f + 12f, r * 52f + 10f, col * 64f + 52f, r * 52f + 42f, p)
            }
        }
        // Colorful bazaar awning band
        for (i in 0 until 8) {
            p.color = if (i % 2 == 0) Color.rgb(218, 66, 56) else Color.rgb(245, 240, 232)
            c.drawRect(i * 32f, 164f, (i + 1) * 32f, 186f, p)
        }
        // Bright shopfront windows below
        p.color = Color.rgb(252, 224, 160)
        c.drawRect(18f, 194f, 116f, 246f, p)
        c.drawRect(140f, 194f, 238f, 246f, p)
        return bmp
    }

    private fun createPrecinctWallBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(220, 226, 236))
        val p = Paint()
        // Cobalt police stripe
        p.color = Color.rgb(24, 72, 168)
        c.drawRect(0f, 156f, size.toFloat(), 184f, p)
        p.color = Color.rgb(244, 186, 28)
        c.drawRect(0f, 184f, size.toFloat(), 192f, p)
        // Upper tactical windows
        p.color = Color.rgb(54, 96, 152)
        for (col in 0 until 4) {
            c.drawRect(col * 64f + 10f, 34f, col * 64f + 54f, 126f, p)
        }
        return bmp
    }

    private fun createPoliceLiveryBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(20, 38, 86))
        val p = Paint()
        p.color = Color.rgb(242, 246, 252)
        c.drawRect(0f, 84f, size.toFloat(), 172f, p)
        p.color = Color.rgb(248, 190, 24)
        c.drawRect(0f, 166f, size.toFloat(), 180f, p)
        p.color = Color.rgb(20, 38, 86)
        p.textSize = 36f
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.CENTER
        c.drawText("POLICE", size / 2f, 140f, p)
        return bmp
    }
}
