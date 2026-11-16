package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine(context: Context) {

    private val appContext = context.applicationContext
    private var soundPool: SoundPool? = null

    private var idClick = 0
    private var idFootstep = 0
    private var idJump = 0
    private var idArrest = 0
    private var idDoor = 0
    private var idMissionStart = 0
    private var idMissionComplete = 0
    private var idGameOver = 0
    private var idEngineLoop = 0
    private var idSirenLoop = 0
    private var idAmbienceLoop = 0
    private var idMusicLoop = 0

    private var engineStreamId = 0
    private var sirenStreamId = 0
    private var ambienceStreamId = 0
    private var musicStreamId = 0

    private var sfxEnabled = true
    private var musicEnabled = true
    private var isPaused = false
    private var isDriving = false
    private var isSirenOn = false
    private var lastEngineSpeedFactor = 0f

    init {
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build()

            idClick = loadSynthWav("sfx_click.wav", synthUiClick())
            idFootstep = loadSynthWav("sfx_step.wav", synthFootstep())
            idJump = loadSynthWav("sfx_jump.wav", synthJump())
            idArrest = loadSynthWav("sfx_arrest.wav", synthArrest())
            idDoor = loadSynthWav("sfx_door.wav", synthDoor())
            idMissionStart = loadSynthWav("sfx_mstart.wav", synthMissionStart())
            idMissionComplete = loadSynthWav("sfx_mwin.wav", synthMissionWin())
            idGameOver = loadSynthWav("sfx_fail.wav", synthGameOver())
            idEngineLoop = loadSynthWav("loop_engine.wav", synthEngineLoop())
            idSirenLoop = loadSynthWav("loop_siren.wav", synthSirenLoop())
            idAmbienceLoop = loadSynthWav("loop_ambience.wav", synthAmbienceLoop())
            idMusicLoop = loadSynthWav("loop_music.wav", synthMusicLoop())
        }
    }

    private fun loadSynthWav(fileName: String, samples: ShortArray): Int {
        val pool = soundPool ?: return 0
        val file = File(appContext.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(buildWavBytes(samples, SAMPLE_RATE))
        }
        return pool.load(file.absolutePath, 1)
    }

    fun updateSettings(music: Boolean, sfx: Boolean) {
        musicEnabled = music
        sfxEnabled = sfx
        val pool = soundPool ?: return
        if (!musicEnabled || isPaused) {
            if (musicStreamId != 0) {
                pool.stop(musicStreamId)
                musicStreamId = 0
            }
        } else if (musicStreamId == 0 && idMusicLoop != 0) {
            musicStreamId = pool.play(idMusicLoop, 0.22f, 0.22f, 0, -1, 1.0f)
        }
        if (!sfxEnabled || isPaused) {
            stopGameplayLoops()
        } else {
            ensureAmbienceRunning()
        }
    }

    fun onEnterGameplay() {
        isPaused = false
        ensureAmbienceRunning()
        if (musicEnabled && musicStreamId == 0 && idMusicLoop != 0) {
            musicStreamId = soundPool?.play(idMusicLoop, 0.22f, 0.22f, 0, -1, 1.0f) ?: 0
        }
    }

    fun onExitGameplay() {
        stopGameplayLoops()
    }

    fun onPause() {
        isPaused = true
        soundPool?.autoPause()
    }

    fun onResume() {
        isPaused = false
        soundPool?.autoResume()
        if (musicEnabled && musicStreamId == 0 && idMusicLoop != 0) {
            musicStreamId = soundPool?.play(idMusicLoop, 0.22f, 0.22f, 0, -1, 1.0f) ?: 0
        }
    }

    private fun ensureAmbienceRunning() {
        val pool = soundPool ?: return
        if (sfxEnabled && !isPaused && ambienceStreamId == 0 && idAmbienceLoop != 0) {
            ambienceStreamId = pool.play(idAmbienceLoop, 0.14f, 0.14f, 0, -1, 1.0f)
        }
    }

    private fun stopGameplayLoops() {
        val pool = soundPool ?: return
        if (engineStreamId != 0) {
            pool.stop(engineStreamId)
            engineStreamId = 0
        }
        if (sirenStreamId != 0) {
            pool.stop(sirenStreamId)
            sirenStreamId = 0
        }
        if (ambienceStreamId != 0) {
            pool.stop(ambienceStreamId)
            ambienceStreamId = 0
        }
        isDriving = false
        isSirenOn = false
    }

    fun updateVehicleAudio(driving: Boolean, speedNormalized: Float, sirenOn: Boolean) {
        val pool = soundPool ?: return
        if (!sfxEnabled || isPaused) return

        if (driving && !isDriving) {
            isDriving = true
            if (idEngineLoop != 0 && engineStreamId == 0) {
                engineStreamId = pool.play(idEngineLoop, 0.34f, 0.34f, 1, -1, 0.85f)
            }
        } else if (!driving && isDriving) {
            isDriving = false
            if (engineStreamId != 0) {
                pool.stop(engineStreamId)
                engineStreamId = 0
            }
        }

        if (isDriving && engineStreamId != 0) {
            val clamped = speedNormalized.coerceIn(0f, 1f)
            if (kotlin.math.abs(clamped - lastEngineSpeedFactor) > 0.04f) {
                lastEngineSpeedFactor = clamped
                val rate = 0.82f + clamped * 0.95f
                val vol = 0.26f + clamped * 0.22f
                pool.setRate(engineStreamId, rate)
                pool.setVolume(engineStreamId, vol, vol)
            }
        }

        val wantSiren = driving && sirenOn
        if (wantSiren && !isSirenOn) {
            isSirenOn = true
            if (idSirenLoop != 0 && sirenStreamId == 0) {
                sirenStreamId = pool.play(idSirenLoop, 0.30f, 0.30f, 1, -1, 1.0f)
            }
        } else if (!wantSiren && isSirenOn) {
            isSirenOn = false
            if (sirenStreamId != 0) {
                pool.stop(sirenStreamId)
                sirenStreamId = 0
            }
        }
    }

    fun playClick() {
        if (sfxEnabled && !isPaused && idClick != 0) {
            soundPool?.play(idClick, 0.45f, 0.45f, 1, 0, 1.0f)
        }
    }

    fun playFootstep(isRunning: Boolean) {
        if (sfxEnabled && !isPaused && idFootstep != 0) {
            val vol = if (isRunning) 0.36f else 0.22f
            val rate = if (isRunning) 1.18f else 0.96f
            soundPool?.play(idFootstep, vol, vol, 0, 0, rate)
        }
    }

    fun playJump() {
        if (sfxEnabled && !isPaused && idJump != 0) {
            soundPool?.play(idJump, 0.40f, 0.40f, 1, 0, 1.0f)
        }
    }

    fun playArrest() {
        if (sfxEnabled && !isPaused && idArrest != 0) {
            soundPool?.play(idArrest, 0.65f, 0.65f, 2, 0, 1.0f)
        }
    }

    fun playVehicleDoor() {
        if (sfxEnabled && !isPaused && idDoor != 0) {
            soundPool?.play(idDoor, 0.48f, 0.48f, 1, 0, 1.0f)
        }
    }

    fun playMissionStart() {
        if (sfxEnabled && !isPaused && idMissionStart != 0) {
            soundPool?.play(idMissionStart, 0.55f, 0.55f, 2, 0, 1.0f)
        }
    }

    fun playMissionComplete() {
        if (sfxEnabled && !isPaused && idMissionComplete != 0) {
            soundPool?.play(idMissionComplete, 0.65f, 0.65f, 2, 0, 1.0f)
        }
    }

    fun playGameOver() {
        if (sfxEnabled && !isPaused && idGameOver != 0) {
            soundPool?.play(idGameOver, 0.55f, 0.55f, 2, 0, 1.0f)
        }
    }

    fun release() {
        stopGameplayLoops()
        if (musicStreamId != 0) {
            soundPool?.stop(musicStreamId)
            musicStreamId = 0
        }
        soundPool?.release()
        soundPool = null
    }

    companion object {
        private const val SAMPLE_RATE = 22050

        private fun buildWavBytes(samples: ShortArray, sampleRate: Int): ByteArray {
            val dataBytes = samples.size * 2
            val out = ByteArrayOutputStream(44 + dataBytes)
            fun writeStr(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
            fun writeInt(v: Int) {
                out.write(v and 0xFF)
                out.write((v shr 8) and 0xFF)
                out.write((v shr 16) and 0xFF)
                out.write((v shr 24) and 0xFF)
            }
            fun writeShort(v: Int) {
                out.write(v and 0xFF)
                out.write((v shr 8) and 0xFF)
            }
            writeStr("RIFF")
            writeInt(36 + dataBytes)
            writeStr("WAVE")
            writeStr("fmt ")
            writeInt(16)
            writeShort(1) // PCM
            writeShort(1) // mono
            writeInt(sampleRate)
            writeInt(sampleRate * 2)
            writeShort(2)
            writeShort(16)
            writeStr("data")
            writeInt(dataBytes)
            for (s in samples) {
                writeShort(s.toInt())
            }
            return out.toByteArray()
        }

        private fun synthUiClick(): ShortArray {
            val n = (SAMPLE_RATE * 0.06).toInt()
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val env = exp(-t * 55.0)
                val sig = sin(2 * PI * 920.0 * t) * 0.5 + sin(2 * PI * 1480.0 * t) * 0.3
                (sig * env * 22000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthFootstep(): ShortArray {
            val n = (SAMPLE_RATE * 0.09).toInt()
            val rnd = Random(42)
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val env = exp(-t * 42.0)
                val thud = sin(2 * PI * (120.0 - t * 400.0) * t) * 0.7
                val grit = (rnd.nextDouble() * 2.0 - 1.0) * 0.35 * exp(-t * 60.0)
                ((thud + grit) * env * 20000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthJump(): ShortArray {
            val n = (SAMPLE_RATE * 0.16).toInt()
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val freq = 220.0 + t * 900.0
                val env = exp(-t * 14.0)
                (sin(2 * PI * freq * t) * env * 18000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthArrest(): ShortArray {
            val n = (SAMPLE_RATE * 0.36).toInt()
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val thump = sin(2 * PI * 130.0 * t) * exp(-t * 16.0) * 0.65
                val chime = (sin(2 * PI * 660.0 * t) + sin(2 * PI * 990.0 * t) * 0.5) * exp(-t * 9.0) * 0.5
                ((thump + chime) * 24000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthDoor(): ShortArray {
            val n = (SAMPLE_RATE * 0.15).toInt()
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val env = exp(-t * 28.0)
                val metallic = sin(2 * PI * 190.0 * t) * 0.6 + sin(2 * PI * 410.0 * t) * 0.35
                (metallic * env * 21000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthMissionStart(): ShortArray {
            val n = (SAMPLE_RATE * 0.55).toInt()
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val idx = ((t / 0.55) * 4).toInt().coerceIn(0, 3)
                val localT = t - idx * (0.55 / 4.0)
                val env = exp(-localT * 8.0)
                val sig = sin(2 * PI * notes[idx] * t) + 0.35 * sin(2 * PI * notes[idx] * 2.0 * t)
                (sig * env * 15000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthMissionWin(): ShortArray {
            val n = (SAMPLE_RATE * 0.90).toInt()
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.50)
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val idx = ((t / 0.90) * 5).toInt().coerceIn(0, 4)
                val localT = t - idx * 0.18
                val env = exp(-localT * 5.0)
                val sig = sin(2 * PI * notes[idx] * t) + 0.4 * sin(2 * PI * notes[idx] * 1.5 * t)
                (sig * env * 16000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthGameOver(): ShortArray {
            val n = (SAMPLE_RATE * 0.70).toInt()
            val notes = doubleArrayOf(392.0, 349.23, 311.13, 261.63)
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val idx = ((t / 0.70) * 4).toInt().coerceIn(0, 3)
                val localT = t - idx * 0.175
                val env = exp(-localT * 5.0)
                val sig = sin(2 * PI * notes[idx] * t)
                (sig * env * 17000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthEngineLoop(): ShortArray {
            // Seamless 1.0s low-frequency rumble loop
            val n = SAMPLE_RATE
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val fundamental = 58.0
                val sig = sin(2 * PI * fundamental * t) * 0.45 +
                    sin(2 * PI * fundamental * 2.0 * t) * 0.28 +
                    sin(2 * PI * fundamental * 3.0 * t) * 0.14 +
                    sin(2 * PI * fundamental * 4.0 * t) * 0.08
                (sig * 15000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthSirenLoop(): ShortArray {
            // Smooth 1.2s two-tone police wail/hi-lo loop
            val n = (SAMPLE_RATE * 1.2).toInt()
            var phase = 0.0
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val lfo = (sin(2 * PI * (t / 1.2) - PI / 2.0) + 1.0) * 0.5
                val freq = 620.0 + lfo * 340.0
                phase += 2 * PI * freq / SAMPLE_RATE.toDouble()
                val sig = sin(phase) * 0.72 + sin(phase * 2.0) * 0.18
                (sig * 14000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthAmbienceLoop(): ShortArray {
            val n = SAMPLE_RATE * 2
            val rnd = Random(7)
            var lp = 0.0
            return ShortArray(n) { i ->
                val white = rnd.nextDouble() * 2.0 - 1.0
                lp = lp * 0.97 + white * 0.03
                val hum = sin(2 * PI * 90.0 * (i / SAMPLE_RATE.toDouble())) * 0.05
                ((lp * 0.7 + hum) * 8000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }

        private fun synthMusicLoop(): ShortArray {
            // Upbeat 4-bar tactical bass + arpeggio synth loop (seamless 4.0s at 120 BPM)
            val dur = 4.0
            val n = (SAMPLE_RATE * dur).toInt()
            val bassNotes = doubleArrayOf(110.0, 110.0, 130.81, 98.0) // A2, A2, C3, G2
            val arpNotes = doubleArrayOf(440.0, 523.25, 659.25, 523.25, 440.0, 587.33, 659.25, 783.99)
            return ShortArray(n) { i ->
                val t = i / SAMPLE_RATE.toDouble()
                val beat = (t * 2.0).toInt() % 8
                val bar = (t.toInt()) % 4
                val subBeatT = (t * 2.0) % 1.0
                val bassFreq = bassNotes[bar]
                val bassEnv = exp(-subBeatT * 3.8)
                val bass = (sin(2 * PI * bassFreq * t) + 0.35 * sin(2 * PI * bassFreq * 2.0 * t)) * bassEnv * 0.45

                val arpIdx = ((t * 4.0).toInt()) % arpNotes.size
                val arpT = (t * 4.0) % 1.0
                val arpEnv = exp(-arpT * 6.5)
                val arp = sin(2 * PI * arpNotes[arpIdx] * t) * arpEnv * 0.22

                val kickEnv = if (beat % 2 == 0) exp(-subBeatT * 18.0) else 0.0
                val kick = sin(2 * PI * (65.0 + 90.0 * exp(-subBeatT * 25.0)) * subBeatT) * kickEnv * 0.35

                ((bass + arp + kick) * 14500).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
    }
}
