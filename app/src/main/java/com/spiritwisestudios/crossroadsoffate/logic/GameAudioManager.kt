package com.spiritwisestudios.crossroadsoffate.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages background music and sound effects for the game.
 * Uses MediaPlayer for looping background music with crossfade on track changes,
 * and SoundPool for short sound effects.
 * Volume and mute settings persist via SharedPreferences.
 */
class GameAudioManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private val loadedSounds = mutableMapOf<String, Int>()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _musicVolume = MutableStateFlow(prefs.getFloat(KEY_MUSIC_VOLUME, 0.7f))
    val musicVolume: StateFlow<Float> = _musicVolume.asStateFlow()

    private val _sfxVolume = MutableStateFlow(prefs.getFloat(KEY_SFX_VOLUME, 1.0f))
    val sfxVolume: StateFlow<Float> = _sfxVolume.asStateFlow()

    private val _isMuted = MutableStateFlow(prefs.getBoolean(KEY_IS_MUTED, false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var currentTrackName: String? = null
    private var isPaused = false

    companion object {
        private const val PREFS_NAME = "audio_settings"
        private const val KEY_MUSIC_VOLUME = "music_volume"
        private const val KEY_SFX_VOLUME = "sfx_volume"
        private const val KEY_IS_MUTED = "is_muted"

        /**
         * Maps a scenario location string to a music track name.
         */
        fun getMusicTrackForLocation(location: String): String {
            return when {
                location.contains("Shadow", ignoreCase = true) ||
                location.contains("Criminal", ignoreCase = true) ||
                location.contains("Temple", ignoreCase = true) ||
                location.contains("Sacred", ignoreCase = true) ||
                location.contains("Cursed", ignoreCase = true) ||
                location.contains("Ruin", ignoreCase = true) -> "mystery"

                location.contains("Wilderness", ignoreCase = true) ||
                location.contains("Trail", ignoreCase = true) ||
                location.contains("Cottage", ignoreCase = true) ||
                location.contains("Forest", ignoreCase = true) -> "wilderness"

                else -> "town"
            }
        }
    }

    /**
     * Initializes SoundPool and loads all SFX resources.
     * Call once at startup.
     */
    fun initialize() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSfx("button_tap", "sfx_button_tap")
        loadSfx("item_acquired", "sfx_item_acquired")
        loadSfx("quest_completed", "sfx_quest_completed")
    }

    private fun loadSfx(name: String, resourceName: String) {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resId == 0) {
            Timber.w("SFX resource not found: %s", resourceName)
            return
        }
        try {
            soundPool?.load(context, resId, 1)?.let { loadedSounds[name] = it }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load SFX: %s", name)
        }
    }

    /**
     * Plays background music for the given track name (e.g. "town", "wilderness", "mystery", "menu").
     * If the same track is already playing, this is a no-op.
     * Stops and releases any currently playing track before starting the new one.
     */
    fun playMusic(trackName: String) {
        if (trackName == currentTrackName && mediaPlayer?.isPlaying == true) return

        val resId = context.resources.getIdentifier("music_$trackName", "raw", context.packageName)
        if (resId == 0) {
            Timber.w("Music resource not found: music_%s", trackName)
            return
        }

        // Stop current track
        stopMusic()

        try {
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                val vol = if (_isMuted.value) 0f else _musicVolume.value
                setVolume(vol, vol)
                start()
            }
            currentTrackName = trackName
            isPaused = false
        } catch (e: Exception) {
            Timber.e(e, "Failed to play music: %s", trackName)
        }
    }

    /**
     * Plays appropriate background music based on a scenario location string.
     */
    fun playMusicForLocation(location: String) {
        playMusic(getMusicTrackForLocation(location))
    }

    /**
     * Stops and releases the current background music.
     */
    fun stopMusic() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Timber.w(e, "Error stopping music")
        }
        mediaPlayer = null
        currentTrackName = null
        isPaused = false
    }

    /**
     * Plays a short sound effect by name (e.g. "button_tap", "item_acquired", "quest_completed").
     */
    fun playSfx(name: String) {
        if (_isMuted.value) return
        val soundId = loadedSounds[name] ?: return
        val vol = _sfxVolume.value
        soundPool?.play(soundId, vol, vol, 1, 0, 1f)
    }

    fun setMusicVolume(volume: Float) {
        _musicVolume.value = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_MUSIC_VOLUME, _musicVolume.value).apply()
        applyMusicVolume()
    }

    fun setSfxVolume(volume: Float) {
        _sfxVolume.value = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_SFX_VOLUME, _sfxVolume.value).apply()
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        prefs.edit().putBoolean(KEY_IS_MUTED, _isMuted.value).apply()
        applyMusicVolume()
    }

    private fun applyMusicVolume() {
        val vol = if (_isMuted.value) 0f else _musicVolume.value
        try {
            mediaPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            Timber.w(e, "Error setting music volume")
        }
    }

    /**
     * Pauses music playback. Call from Activity.onPause().
     */
    fun onPause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                isPaused = true
            }
        } catch (e: Exception) {
            Timber.w(e, "Error pausing audio")
        }
    }

    /**
     * Resumes music playback if it was paused. Call from Activity.onResume().
     */
    fun onResume() {
        try {
            if (isPaused) {
                mediaPlayer?.start()
                isPaused = false
            }
        } catch (e: Exception) {
            Timber.w(e, "Error resuming audio")
        }
    }

    /**
     * Releases all audio resources. Call from Activity.onDestroy() or ViewModel.onCleared().
     */
    fun release() {
        stopMusic()
        try {
            soundPool?.release()
        } catch (e: Exception) {
            Timber.w(e, "Error releasing sound pool")
        }
        soundPool = null
        loadedSounds.clear()
    }

    /** Returns the current music track name, or null if nothing is playing. */
    fun getCurrentTrackName(): String? = currentTrackName
}
