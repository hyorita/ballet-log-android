package com.hyorita.balletlog.ui.music

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hyorita.balletlog.util.debugLog

/**
 * Plays the 30-second previews Apple serves for catalog tracks.
 *
 * Deliberately minimal: one track at a time, no queue, no background audio.
 * These are previews for deciding whether an album is worth opening in a
 * streaming service — not a playback engine. That is also why this is a plain
 * [MediaPlayer] rather than ExoPlayer: a single AAC/MP4 stream with no
 * buffering policy, no track selection, and no session to expose.
 */
object PreviewPlayer {

    /** Row id of the track currently playing, or null. Compose-observable. */
    var playingId by mutableStateOf<String?>(null)
        private set

    private var player: MediaPlayer? = null

    /**
     * Bumped on every [stop] so a `prepareAsync` callback that lands after the
     * user moved on can tell it belongs to a player nobody is waiting for.
     */
    private var generation = 0

    fun isPlaying(id: String): Boolean = playingId == id

    /** Tapping the track that's already playing stops it. */
    fun toggle(id: String, url: String) {
        if (playingId == id) stop() else play(id, url)
    }

    private fun play(id: String, url: String) {
        stop()
        val token = generation

        runCatching {
            // Deliberately not MediaPlayer().apply { ... }: inside that receiver
            // scope a bare stop() binds to MediaPlayer.stop(), which halts the
            // audio but never clears playingId — the row keeps showing a pause
            // icon over silence. Explicit calls keep the two stops apart.
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mediaPlayer.setDataSource(url)
            mediaPlayer.setOnPreparedListener {
                if (token == generation) it.start() else it.release()
            }
            mediaPlayer.setOnCompletionListener { this@PreviewPlayer.stop() }
            mediaPlayer.setOnErrorListener { _, _, _ -> this@PreviewPlayer.stop(); true }
            mediaPlayer.prepareAsync()
            player = mediaPlayer
            playingId = id
        }.onFailure {
            debugLog("PreviewPlayer", "playback failed for $url", it)
            stop()
        }
    }

    fun stop() {
        generation++
        player?.runCatching { release() }
        player = null
        playingId = null
    }
}
