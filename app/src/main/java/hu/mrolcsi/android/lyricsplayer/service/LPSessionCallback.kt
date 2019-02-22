package hu.mrolcsi.android.lyricsplayer.service

import android.annotation.TargetApi
import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.AudioAttributesCompat
import hu.mrolcsi.android.lyricsplayer.extensions.album
import hu.mrolcsi.android.lyricsplayer.extensions.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.artist
import hu.mrolcsi.android.lyricsplayer.extensions.duration
import hu.mrolcsi.android.lyricsplayer.extensions.title
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService.Companion.ACTION_START_UPDATER
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService.Companion.ACTION_STOP_UPDATER
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

open class LPSessionCallback(
  private val context: Context,
  private val session: MediaSessionCompat
) : MediaSessionCompat.Callback() {

  // Player
  private var mMediaPlayer: MediaPlayer = MediaPlayer().apply { setOnCompletionListener { onStop() } }
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

  // Player state polling
  private val mUpdaterEnabled = AtomicBoolean(false)
  private val mUpdateHandler = Handler()
  private val mUpdateRunnable = object : Runnable {
    override fun run() {
      mLastState = PlaybackStateCompat.Builder(mLastState).setState(
        mLastState.state,
        mMediaPlayer.currentPosition.toLong(),
        1f
      ).build()

      if (mUpdaterEnabled.get()) {
        mUpdateHandler.postDelayed(this, UPDATE_FREQUENCY)
      }
    }
  }

  // Audio focus handling
  private lateinit var mAudioFocusRequest: AudioFocusRequest
  private val mAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
    when (it) {
      AudioManager.AUDIOFOCUS_LOSS -> onPause()
      else -> {
        // TODO: change playback state depending on focus?
        //    Like, lower volume when a notification sound plays?
      }
    }
  }
  private val mBecomingNoisyReceiver by lazy {
    BecomingNoisyReceiver(context, session.sessionToken)
  }

  override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
    // Reset media player before loading
    mMediaPlayer.stop()
    mMediaPlayer.reset()

    Log.v(LOG_TAG, "Loading media into Player: $mediaId")

    // Using mediaId as path
    mMediaPlayer.setDataSource(mediaId)
    mMediaPlayer.prepare()  // or prepareAsync()?

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
    mLastState = PlaybackStateCompat.Builder(mLastState).setState(
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

    val result = if (Build.VERSION.SDK_INT >= 26) {
      requestAudioFocusApi26()
    } else {
      requestAudioFocusApi21()
    }

    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      // Set the session active
      session.isActive = true

      // start the player (custom call)
      mMediaPlayer.start()

      // Register BECOME_NOISY BroadcastReceiver
      mBecomingNoisyReceiver.register()

      // Update playback state
      mLastState = PlaybackStateCompat.Builder(mLastState).setState(
        PlaybackStateCompat.STATE_PLAYING,
        mMediaPlayer.currentPosition.toLong(),
        1f
      ).build()
    }
  }

  @Suppress("DEPRECATION")
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private fun requestAudioFocusApi21(): Int {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Request audio focus for playback, this registers the afChangeListener
    return am.requestAudioFocus(
      mAudioFocusChangeListener,
      AudioAttributesCompat.CONTENT_TYPE_MUSIC,
      AudioManager.AUDIOFOCUS_GAIN
    )
  }

  @TargetApi(Build.VERSION_CODES.O)
  private fun requestAudioFocusApi26(): Int {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Request audio focus for playback, this registers the afChangeListener
    mAudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
      setOnAudioFocusChangeListener(mAudioFocusChangeListener)
      setAudioAttributes(AudioAttributes.Builder().run {
        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        build()
      })
      build()
    }
    return am.requestAudioFocus(mAudioFocusRequest)
  }

  override fun onPause() {
    Log.v(LOG_TAG, "onPause(): $mMediaPlayer")

    // pause the player (custom call)
    mMediaPlayer.pause()

    // unregister BECOME_NOISY BroadcastReceiver
    mBecomingNoisyReceiver.unregister()

    // Update metadata and state
    mLastState = PlaybackStateCompat.Builder(mLastState).setState(
      PlaybackStateCompat.STATE_PAUSED,
      mMediaPlayer.currentPosition.toLong(),
      1f
    ).build()
    mLastPlayed.lastPlayedPosition = mMediaPlayer.currentPosition.toLong()
  }

  override fun onStop() {
    Log.v(LOG_TAG, "onStop(): $mMediaPlayer")

    // Abandon audio focus
    if (Build.VERSION.SDK_INT >= 26) {
      abandonAudioFocusApi26()
    } else {
      abandonAudioFocusApi21()
    }

    // unregister BECOME_NOISY BroadcastReceiver
    mBecomingNoisyReceiver.unregister()

    // Set the session inactive  (and update metadata and state)
    session.isActive = false

    mLastState = PlaybackStateCompat.Builder(mLastState).setState(
      PlaybackStateCompat.STATE_STOPPED,
      0,
      1f
    ).build()
    mLastPlayed.lastPlayedPosition = 0

    // stop the player (custom call)
    if (mMediaPlayer.isPlaying) {
      mMediaPlayer.stop()
    }

    // Cancel Handler
    mUpdateHandler.removeCallbacks(mUpdateRunnable)
  }

  @Suppress("DEPRECATION")
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private fun abandonAudioFocusApi21(): Int {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return am.abandonAudioFocus(mAudioFocusChangeListener)
  }

  @TargetApi(Build.VERSION_CODES.O)
  private fun abandonAudioFocusApi26(): Int {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return am.abandonAudioFocusRequest(mAudioFocusRequest)
  }

  override fun onSeekTo(pos: Long) {
    Log.v(LOG_TAG, "onSeekTo(${pos.toInt()}): $mMediaPlayer")

    mMediaPlayer.seekTo(pos.toInt())
    mLastState = PlaybackStateCompat.Builder(mLastState).setState(
      mLastState.state,
      mMediaPlayer.currentPosition.toLong(),
      1f
    ).build()
  }

  companion object {
    private const val LOG_TAG = "LPSessionCallback"

    private const val UPDATE_FREQUENCY: Long = 500  // in milliseconds
  }
}