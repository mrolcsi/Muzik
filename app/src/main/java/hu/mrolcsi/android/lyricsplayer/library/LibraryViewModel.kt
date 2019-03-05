package hu.mrolcsi.android.lyricsplayer.library

import android.app.Application
import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService

abstract class LibraryViewModel(app: Application) : AndroidViewModel(app) {

  val mediaController = MutableLiveData<MediaControllerCompat?>()

  val currentMediaMetadata = MutableLiveData<MediaMetadataCompat?>()
  val currentPlaybackState = MutableLiveData<PlaybackStateCompat?>()

  protected val mMediaBrowser: MediaBrowserCompat by lazy {
    MediaBrowserCompat(
      getApplication(),
      ComponentName(getApplication(), LPPlayerService::class.java),
      mConnectionCallbacks,
      null // optional Bundle
    )
  }

  private val mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {
      mMediaBrowser.sessionToken.also { token ->

        Log.v(LOG_TAG, "Connected to session: $token")

        // Attach a controller to this MediaSession
        val controller = MediaControllerCompat(getApplication(), token).apply {
          // Register callbacks to watch for changes
          registerCallback(object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
              currentMediaMetadata.postValue(metadata)
            }

            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
              currentPlaybackState.postValue(state)
            }
          })
        }
        mediaController.postValue(controller)

        // Set initial state, and metadata
        currentPlaybackState.postValue(controller.playbackState)
        currentMediaMetadata.postValue(controller.metadata)
      }
    }
  }

  fun connect() {
    Log.v(LOG_TAG, "connect($this)")
    mMediaBrowser.connect()
  }

  fun disconnect() {
    Log.v(LOG_TAG, "disconnect($this)")
    mMediaBrowser.disconnect()
  }

  override fun onCleared() {
    super.onCleared()
    Log.v(LOG_TAG, "onCleared($this): disconnect from MediaBrowser")
    mMediaBrowser.disconnect()
  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "LibraryViewModel"
  }
}