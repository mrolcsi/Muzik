package hu.mrolcsi.android.lyricsplayer.service

import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

class LPSessionCallback : MediaSessionCompat.Callback() {

  private var mMediaPlayer = MediaPlayer()

  override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
    Log.v(LOG_TAG, "Loading media into Player: $mediaId")

    // Using mediaId as path
    mMediaPlayer.setDataSource(mediaId)
    mMediaPlayer.prepare()  // or prepareAsync()?

    Log.v(LOG_TAG, "Player prepared.")
  }

  override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
    onPrepareFromMediaId(mediaId, extras)
    onPlay()
  }

  override fun onPlay() {
    super.onPlay()

    mMediaPlayer.start()

    Log.v(LOG_TAG, "Player started: $mMediaPlayer")
  }

  override fun onPause() {
    super.onPause()

    mMediaPlayer.pause()
  }

  override fun onStop() {
    super.onStop()

    mMediaPlayer.stop()
  }

  companion object {
    private const val LOG_TAG = "LPSessionCallback"
  }
}