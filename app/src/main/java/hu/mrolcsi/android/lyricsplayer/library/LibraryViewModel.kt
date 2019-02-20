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

        Log.v(this.toString(), "Controller ready")
        mediaController.postValue(
          MediaControllerCompat(getApplication(), token).apply {
            registerCallback(object : MediaControllerCompat.Callback() {
              override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                Log.v(this.toString(), "onMetadataChanged()")
                currentMediaMetadata.postValue(metadata)
              }

              override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                Log.v(this.toString(), "onPlaybackStateChanged()")
                currentPlaybackState.postValue(state)
              }
            })
          }
        )
      }
    }
  }

  fun connect() {
    Log.v(this.toString(), "connect()")
    mMediaBrowser.connect()
  }

  fun disconnect() {
    Log.v(this.toString(), "disconnect()")
    mMediaBrowser.disconnect()
  }

  override fun onCleared() {
    super.onCleared()
    Log.v(this.toString(), "onCleared(): disconnect from MediaBrowser")
    mMediaBrowser.disconnect()
  }

  companion object {
    private const val LOG_TAG = "LibraryViewModel"
  }
}