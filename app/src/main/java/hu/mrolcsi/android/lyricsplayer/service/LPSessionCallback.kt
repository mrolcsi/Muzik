package hu.mrolcsi.android.lyricsplayer.service

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import hu.mrolcsi.android.lyricsplayer.extensions.album
import hu.mrolcsi.android.lyricsplayer.extensions.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.artist
import hu.mrolcsi.android.lyricsplayer.extensions.duration
import hu.mrolcsi.android.lyricsplayer.extensions.title
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService.Companion.ACTION_START_UPDATER
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService.Companion.ACTION_STOP_UPDATER
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

class LPSessionCallback(
  context: Context,
  private val session: MediaSessionCompat
) : MediaSessionCompat.Callback() {

  private var mMediaPlayer: MediaPlayer? = null
  private var mLastState by Delegates.observable(
    PlaybackStateCompat.Builder().apply {
      setActions(
        PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE
      )
    }.build()
  ) { _, _, new ->
    session.setPlaybackState(new)
  }

  private val mLastPlayed = LastPlayedSetting(context)

  private val mUpdaterEnabled = AtomicBoolean(false)
  private val mUpdateHandler = Handler()
  private val mUpdateRunnable = object : Runnable {
    override fun run() {
      mLastState = PlaybackStateCompat.Builder(mLastState)
        .setState(
          mLastState.state,
          mMediaPlayer?.currentPosition?.toLong() ?: 0,
          1f
        ).build()

      if (mUpdaterEnabled.get()) {
        mUpdateHandler.postDelayed(this, UPDATE_FREQUENCY)
      }
    }
  }

  override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
    if (mMediaPlayer == null) {
      mMediaPlayer = MediaPlayer()
    }

    // Reset media player before loading
    mMediaPlayer?.stop()
    mMediaPlayer?.reset()

    Log.v(LOG_TAG, "Loading media into Player: $mediaId")

    // Using mediaId as path
    mMediaPlayer?.setDataSource(mediaId)
    mMediaPlayer?.prepare()  // or prepareAsync()?

    Log.v(LOG_TAG, "Player prepared.")

    // Save as Last Played
    mLastPlayed.lastPlayedMedia = mediaId

    // Load metadata
    val retriever = MediaMetadataRetriever().apply {
      setDataSource(mediaId)
    }
    val metadataBuilder = MediaMetadataCompat.Builder().apply {
      artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
      album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
      title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
      duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
      retriever.embeddedPicture?.let { albumArt = BitmapFactory.decodeByteArray(it, 0, it.size) }
      // TODO: other metadata
    }
    session.setMetadata(metadataBuilder.build())

    // Update playback state (Let's say we're paused)
    mLastState = PlaybackStateCompat.Builder(mLastState)
      .setState(
        PlaybackStateCompat.STATE_PAUSED,
        0,
        1f
      )
      .build()
  }

  override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
    onPrepareFromMediaId(mediaId, extras)
    onPlay()
  }

  override fun onCustomAction(action: String?, extras: Bundle?) {
    when (action) {
      ACTION_START_UPDATER -> if (!mUpdaterEnabled.getAndSet(true)) {
        mUpdateHandler.post(mUpdateRunnable)
      }
      ACTION_STOP_UPDATER -> mUpdaterEnabled.set(false)
      else -> super.onCustomAction(action, extras)
    }
  }

  // -- GENERAL PLAYBACK STATES

  override fun onPlay() {
    Log.v(LOG_TAG, "onPlay(): $mMediaPlayer")

    // Start player, and update state
    mMediaPlayer?.start()


    mLastState = PlaybackStateCompat.Builder(mLastState)
      .setState(
        PlaybackStateCompat.STATE_PLAYING,
        mMediaPlayer?.currentPosition?.toLong() ?: 0,
        1f
      ).build()
  }

  override fun onPause() {
    Log.v(LOG_TAG, "onPause(): $mMediaPlayer")

    mMediaPlayer?.pause()

    mLastState = PlaybackStateCompat.Builder(mLastState)
      .setState(
        PlaybackStateCompat.STATE_PAUSED,
        mMediaPlayer?.currentPosition?.toLong() ?: 0,
        1f
      ).build()

    mLastPlayed.lastPlayedPosition = mMediaPlayer?.currentPosition?.toLong() ?: 0
  }

  override fun onStop() {
    Log.v(LOG_TAG, "onStop(): $mMediaPlayer")

    mMediaPlayer?.stop()
    mLastState = PlaybackStateCompat.Builder(mLastState)
      .setState(
        PlaybackStateCompat.STATE_STOPPED,
        mMediaPlayer?.currentPosition?.toLong() ?: 0,
        1f
      ).build()

    // Cancel Handler
    mUpdateHandler.removeCallbacks(mUpdateRunnable)

    mLastPlayed.lastPlayedPosition = mMediaPlayer?.currentPosition?.toLong() ?: 0

    mMediaPlayer?.release()
    mMediaPlayer = null
  }

  override fun onSeekTo(pos: Long) {
    Log.v(LOG_TAG, "onSeekTo(${pos.toInt()}): $mMediaPlayer")

    mMediaPlayer?.seekTo(pos.toInt())
    mLastState = PlaybackStateCompat.Builder()
      .setState(
        mLastState.state,
        mMediaPlayer?.currentPosition?.toLong() ?: 0,
        1f
      ).build()
  }

  companion object {
    private const val LOG_TAG = "LPSessionCallback"

    private const val UPDATE_FREQUENCY: Long = 500  // in milliseconds
  }
}