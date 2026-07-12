package com.spiritwisestudios.crossroadsoffate.logic

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GameAudioManagerTest {

    private lateinit var audioManager: GameAudioManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        audioManager = GameAudioManager(context)
    }

    // --- getMusicTrackForLocation tests ---

    @Test
    fun getMusicTrackForLocation_townLocations_returnsTown() {
        assertEquals("town", GameAudioManager.getMusicTrackForLocation("Town Square"))
        assertEquals("town", GameAudioManager.getMusicTrackForLocation("Merchant Quarters"))
        assertEquals("town", GameAudioManager.getMusicTrackForLocation("Council Chamber"))
        assertEquals("town", GameAudioManager.getMusicTrackForLocation("Guard Training Grounds"))
    }

    @Test
    fun getMusicTrackForLocation_wildernessLocations_returnsWilderness() {
        assertEquals("wilderness", GameAudioManager.getMusicTrackForLocation("Wilderness Trail"))
        assertEquals("wilderness", GameAudioManager.getMusicTrackForLocation("Mentor's Cottage"))
        assertEquals("wilderness", GameAudioManager.getMusicTrackForLocation("Deep Forest"))
    }

    @Test
    fun getMusicTrackForLocation_mysteryLocations_returnsMystery() {
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Shadowed Alley"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Criminal Hideout"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Sacred Temple"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Cursed Ruins"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Ancient Ruins"))
    }

    @Test
    fun getMusicTrackForLocation_unknownLocation_defaultsToTown() {
        assertEquals("town", GameAudioManager.getMusicTrackForLocation("Unknown Place"))
        assertEquals("town", GameAudioManager.getMusicTrackForLocation(""))
    }

    @Test
    fun getMusicTrackForLocation_endgameLocations_returnMystery() {
        // Every endgame plane in scenarios.json must map away from town music
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Underground Hideout"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Underground Syndicate"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Abyssal Throne"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Infernal Portal"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Celestial Door"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Eternal Celestial Court"))
        assertEquals("mystery", GameAudioManager.getMusicTrackForLocation("Future's Threshold"))
    }

    // --- Lifecycle / background playback tests ---

    @Test
    fun playMusic_whileBackgrounded_isDeferredUntilResume() {
        audioManager.onPause()

        // A coroutine finishing after the user switched apps must not start music
        audioManager.playMusic("town")
        assertNull(audioManager.currentTrack.value)

        audioManager.onResume()
        assertEquals("town", audioManager.currentTrack.value)
    }

    @Test
    fun stopMusic_whileBackgrounded_dropsTheDeferredTrack() {
        audioManager.onPause()
        audioManager.playMusic("town")
        audioManager.stopMusic()

        audioManager.onResume()
        assertNull("A stopped track must not restart on resume", audioManager.currentTrack.value)
    }

    @Test
    fun playMusic_inForeground_tracksCurrentTrack() {
        audioManager.playMusic("wilderness")
        assertEquals("wilderness", audioManager.currentTrack.value)

        audioManager.stopMusic()
        assertNull(audioManager.currentTrack.value)
    }

    // --- Volume and mute state tests ---

    @Test
    fun defaultMusicVolume_is0point7() {
        assertEquals(0.7f, audioManager.musicVolume.value, 0.01f)
    }

    @Test
    fun defaultSfxVolume_is1point0() {
        assertEquals(1.0f, audioManager.sfxVolume.value, 0.01f)
    }

    @Test
    fun defaultIsMuted_isFalse() {
        assertFalse(audioManager.isMuted.value)
    }

    @Test
    fun setMusicVolume_updatesState() {
        audioManager.setMusicVolume(0.3f)
        assertEquals(0.3f, audioManager.musicVolume.value, 0.01f)
    }

    @Test
    fun setMusicVolume_clampsToRange() {
        audioManager.setMusicVolume(1.5f)
        assertEquals(1.0f, audioManager.musicVolume.value, 0.01f)

        audioManager.setMusicVolume(-0.5f)
        assertEquals(0.0f, audioManager.musicVolume.value, 0.01f)
    }

    @Test
    fun setSfxVolume_updatesState() {
        audioManager.setSfxVolume(0.5f)
        assertEquals(0.5f, audioManager.sfxVolume.value, 0.01f)
    }

    @Test
    fun setSfxVolume_clampsToRange() {
        audioManager.setSfxVolume(2.0f)
        assertEquals(1.0f, audioManager.sfxVolume.value, 0.01f)

        audioManager.setSfxVolume(-1.0f)
        assertEquals(0.0f, audioManager.sfxVolume.value, 0.01f)
    }

    @Test
    fun toggleMute_togglesState() {
        assertFalse(audioManager.isMuted.value)
        audioManager.toggleMute()
        assertTrue(audioManager.isMuted.value)
        audioManager.toggleMute()
        assertFalse(audioManager.isMuted.value)
    }

    // --- Persistence tests ---

    @Test
    fun setMusicVolume_persistsToPreferences() {
        audioManager.setMusicVolume(0.4f)
        val prefs = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
        assertEquals(0.4f, prefs.getFloat("music_volume", 0f), 0.01f)
    }

    @Test
    fun setSfxVolume_persistsToPreferences() {
        audioManager.setSfxVolume(0.6f)
        val prefs = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
        assertEquals(0.6f, prefs.getFloat("sfx_volume", 0f), 0.01f)
    }

    @Test
    fun toggleMute_persistsToPreferences() {
        audioManager.toggleMute()
        val prefs = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("is_muted", false))
    }

    @Test
    fun constructor_loadsPersistedSettings() {
        // Set values in prefs first
        val prefs = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
        prefs.edit().putFloat("music_volume", 0.2f).putFloat("sfx_volume", 0.8f).putBoolean("is_muted", true).apply()

        // Create new instance - should load from prefs
        val newManager = GameAudioManager(context)
        assertEquals(0.2f, newManager.musicVolume.value, 0.01f)
        assertEquals(0.8f, newManager.sfxVolume.value, 0.01f)
        assertTrue(newManager.isMuted.value)
    }

}
