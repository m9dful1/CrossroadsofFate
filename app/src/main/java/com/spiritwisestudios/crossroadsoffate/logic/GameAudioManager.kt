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
 * Uses MediaPlayer for looping background music (tracks switch with a hard cut)
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

    private val _currentTrack = MutableStateFlow<String?>(null)
    /** Track whose playback last started, or null when stopped. */
    val currentTrack: StateFlow<String?> = _currentTrack.asStateFlow()

    private var isPaused = false

    // While the activity is paused, play requests are deferred here instead of
    // starting playback behind the lock screen; onResume() starts the track.
    private var isInForeground = true
    private var pendingTrackName: String? = null

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

                // Endgame planes share the mystery track — town music would
                // break the tone of the Abyssal/Celestial/Infernal finale
                location.contains("Underground", ignoreCase = true) ||
                location.contains("Abyssal", ignoreCase = true) ||
                location.contains("Infernal", ignoreCase = true) ||
                location.contains("Celestial", ignoreCase = true) ||
                location.contains("Threshold", ignoreCase = true) -> "mystery"

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
        if (!isInForeground) {
            // Never start playback while backgrounded (e.g. a save/load
            // coroutine finishing after the user switched apps)
            pendingTrackName = trackName
            return
        }
        if (trackName == _currentTrack.value && isPlayingSafely()) return

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
            _currentTrack.value = trackName
            isPaused = false
        } catch (e: Exception) {
            Timber.e(e, "Failed to play music: %s", trackName)
        }
    }

    /** isPlaying throws IllegalStateException on a released or errored player. */
    private fun isPlayingSafely(): Boolean = try {
        mediaPlayer?.isPlaying == true
    } catch (e: Exception) {
        Timber.w(e, "MediaPlayer in unusable state")
        false
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
        mediaPlayer?.let { player ->
            try {
                if (isPlayingSafely()) player.stop()
            } catch (e: Exception) {
                Timber.w(e, "Error stopping music")
            }
            try {
                player.release()
            } catch (e: Exception) {
                Timber.w(e, "Error releasing music player")
            }
        }
        mediaPlayer = null
        _currentTrack.value = null
        pendingTrackName = null
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
     * Pauses music playback and defers any further play requests.
     * Call from Activity.onPause().
     */
    fun onPause() {
        isInForeground = false
        try {
            if (isPlayingSafely()) {
                mediaPlayer?.pause()
                isPaused = true
            }
        } catch (e: Exception) {
            Timber.w(e, "Error pausing audio")
        }
    }

    /**
     * Resumes paused playback, or starts the track requested while the app
     * was backgrounded. Call from Activity.onResume().
     */
    fun onResume() {
        isInForeground = true
        val pending = pendingTrackName
        pendingTrackName = null
        if (pending != null && pending != _currentTrack.value) {
            playMusic(pending)
            return
        }
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
}
