package hu.mrolcsi.muzik.library

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData

interface SessionViewModel {

  fun connect()
  fun disconnect()

  val mediaController: LiveData<MediaControllerCompat?>
  val currentMediaMetadata: LiveData<MediaMetadataCompat?>
  val currentPlaybackState: LiveData<PlaybackStateCompat?>

  val mediaBrowser: MediaBrowserCompat
}