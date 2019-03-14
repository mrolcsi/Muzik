package hu.mrolcsi.android.lyricsplayer.player

import android.app.Application
import android.os.AsyncTask
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.android.lyricsplayer.extensions.switchMap
import hu.mrolcsi.android.lyricsplayer.library.LibraryViewModel

class PlayerViewModel(app: Application) : LibraryViewModel(app) {

  override fun getLogTag(): String = "PlayerViewModel"

  val currentQueue: LiveData<List<MediaSessionCompat.QueueItem>> = mediaController.switchMap { controller ->
    val liveQueue = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
    AsyncTask.execute { liveQueue.postValue(controller?.queue) }
    liveQueue
  }
}