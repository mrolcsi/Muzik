package hu.mrolcsi.android.lyricsplayer.service

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.android.uamp.media.extensions.album
import com.example.android.uamp.media.extensions.albumArt
import com.example.android.uamp.media.extensions.artist
import com.example.android.uamp.media.extensions.duration
import com.example.android.uamp.media.extensions.title
import kotlin.properties.Delegates

class LPSessionCallback(
  private val session: MediaSessionCompat
) : MediaSessionCompat.Callback() {

  private var mMediaPlayer: MediaPlayer? = null
  private val mPlaybackStateBuilder = PlaybackStateCompat.Builder().apply {
    setActions(
      PlaybackStateCompat.ACTION_PLAY_PAUSE or
          PlaybackStateCompat.ACTION_PLAY or
          PlaybackStateCompat.ACTION_PAUSE
    )
  }
  private var mLastState: PlaybackStateCompat by Delegates.observable(mPlaybackStateBuilder.build()) { _, _, newState ->
    // Update session when state changes
    session.setPlaybackState(newState)
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
  }

  override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
    onPrepareFromMediaId(mediaId, extras)
    onPlay()
  }

  // -- GENERAL PLAYBACK STATES

  override fun onPlay() {
    Log.v(LOG_TAG, "onPlay(): $mMediaPlayer")

    // Start player, and update state
    mMediaPlayer?.start()

    mLastState = mPlaybackStateBuilder.setState(
      PlaybackStateCompat.STATE_PLAYING,
      mLastState.position,
      1f
    ).build()
  }

  override fun onPause() {
    Log.v(LOG_TAG, "onPause(): $mMediaPlayer")

    mMediaPlayer?.pause()
    mLastState = mPlaybackStateBuilder.setState(
      PlaybackStateCompat.STATE_PAUSED,
      mLastState.position,
      1f
    ).build()
  }

  override fun onStop() {
    Log.v(LOG_TAG, "onStop(): $mMediaPlayer")

    mMediaPlayer?.stop()
    mLastState = mPlaybackStateBuilder.setState(
      PlaybackStateCompat.STATE_STOPPED,
      mLastState.position,
      1f
    ).build()

    mMediaPlayer?.release()
    mMediaPlayer = null
  }

  override fun onSeekTo(pos: Long) {
    Log.v(LOG_TAG, "onSeekTo(${pos.toInt()}): $mMediaPlayer")

    mMediaPlayer?.seekTo(pos.toInt())
  }

  companion object {
    private const val LOG_TAG = "LPSessionCallback"
  }
}