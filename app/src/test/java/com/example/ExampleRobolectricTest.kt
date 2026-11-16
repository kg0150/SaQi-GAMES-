package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEngine
import com.example.data.OfficerId
import com.example.game3d.CityWorld
import com.example.game3d.GameSimulation
import com.example.localization.AppLanguage
import com.example.localization.GameStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Patrol City", appName)
    }

    @Test
    fun `hindi and urdu strings contain proper unicode`() {
        val hi = GameStrings.get(AppLanguage.HINDI)
        val ur = GameStrings.get(AppLanguage.URDU)
        assertTrue(hi.motuName.contains("मोटू"))
        assertTrue(hi.patluName.contains("पतलू"))
        assertTrue(ur.motuName.contains("موٹو"))
        assertTrue(ur.patluName.contains("پتلو"))
    }

    @Test
    fun `simulation supports walking officer switch vehicle and mission completion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sound = SoundEngine(context)
        val sim = GameSimulation(CityWorld(), sound)

        sim.startMission(1)
        assertEquals(OfficerId.MOTU, sim.activeOfficer)
        sim.switchOfficer()
        assertEquals(OfficerId.PATLU, sim.activeOfficer)

        // Advance past cinematic intro and walk player
        for (i in 0 until 60) sim.step(0.05f)
        val startX = sim.playerPos.x
        val startZ = sim.playerPos.z
        sim.input.joyY = -1f
        for (i in 0 until 20) sim.step(0.04f)
        sim.input.joyY = 0f
        assertTrue(sim.playerPos.distXZ(startX, startZ) > 0.5f)

        // Move to Mission 1 checkpoint -> Bazaar Market
        val cp = sim.checkpointPos!!
        sim.playerPos.set(cp.x, 0f, cp.z)
        sim.step(0.04f)
        assertEquals(1, sim.objectiveStep)

        // Arrest fleeing criminal
        val crim = sim.criminals.first()
        sim.playerPos.set(crim.pos.x + 1.0f, 0f, crim.pos.z)
        sim.triggerActionOrArrest()
        assertTrue(crim.isApprehended)
        assertEquals(2, sim.objectiveStep)

        // Check on protected civilian -> Mission Complete!
        val civ = sim.civilians.first { it.id == 901 }
        sim.playerPos.set(civ.pos.x + 1.0f, 0f, civ.pos.z)
        sim.triggerActionOrArrest()
        assertNotNull(sim.missionCompletePoints)
        sound.release()
    }
}
