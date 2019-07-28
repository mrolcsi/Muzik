package hu.mrolcsi.muzik.library

import android.app.Application
import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.service.MuzikPlayerService

@Deprecated("Use MediaService and MediaRepository")
abstract class SessionViewModelBase constructor(
  app: Application
) : AndroidViewModel(app), SessionViewModel {

  override val mediaController = MutableLiveData<MediaControllerCompat?>()

  override val currentMediaMetadata = MutableLiveData<MediaMetadataCompat?>()
  override val currentPlaybackState = MutableLiveData<PlaybackStateCompat?>()

  private var mLastMetadata: MediaMetadataCompat? = null

  val mMediaBrowser: MediaBrowserCompat by lazy {
    MediaBrowserCompat(
      getApplication(),
      ComponentName(getApplication(), MuzikPlayerService::class.java),
      mConnectionCallbacks,
      null // optional Bundle
    )
  }

  private val mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {
      mMediaBrowser.sessionToken.also { token ->

        Log.v(getLogTag(), "Connected to session: $token")

        // Attach a controller to this MediaSession
        val controller = MediaControllerCompat(getApplication(), token).apply {
          // Register callbacks to watch for changes
          registerCallback(object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
              metadata?.let {
                Log.v(getLogTag(), "onMetadataChanged(${metadata.description})")

                // Check if metadata has actually changed
                // Only post metadata whn it has an albumArt
                if (metadata.description?.mediaId != mLastMetadata?.description?.mediaId) {
                  currentMediaMetadata.postValue(metadata)
                  // Save as last received metadata
                  mLastMetadata = metadata
                }
              }
            }

            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
              //Log.v(getLogTag(), "onPlaybackStateChanged($state)")/
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

  override fun connect() {
    Log.v(getLogTag(), "connect($this)")
    mMediaBrowser.connect()
  }

  override fun disconnect() {
    Log.v(getLogTag(), "disconnect($this)")
    mMediaBrowser.disconnect()
  }

  override val mediaBrowser get() = mMediaBrowser

  override fun onCleared() {
    super.onCleared()
    Log.v(getLogTag(), "onCleared($this): disconnect from MediaBrowser")
    mMediaBrowser.disconnect()
  }

  abstract fun getLogTag(): String
}