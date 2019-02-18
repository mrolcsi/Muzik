package hu.mrolcsi.android.lyricsplayer.library

import android.app.Application
import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.AndroidViewModel
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService

abstract class LibraryViewModel(app: Application) : AndroidViewModel(app) {

  protected val mMediaBrowser: MediaBrowserCompat by lazy {
    MediaBrowserCompat(
      getApplication(),
      ComponentName(getApplication(), LPPlayerService::class.java),
      mConnectionCallbacks,
      null // optional Bundle
    )
  }

  private val mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    // TODO
  }

  override fun onCleared() {
    super.onCleared()

    mMediaBrowser.disconnect()
  }
}