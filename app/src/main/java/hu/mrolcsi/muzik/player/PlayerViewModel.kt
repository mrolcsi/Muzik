package hu.mrolcsi.muzik.player

import android.app.Application
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import hu.mrolcsi.muzik.library.SessionViewModel

class PlayerViewModel(app: Application) : SessionViewModel(app) {

  val currentQueue: LiveData<List<MediaSessionCompat.QueueItem>> =
    MediatorLiveData<List<MediaSessionCompat.QueueItem>>().apply {
      addSource(mediaController) {
        postValue(it?.queue)
      }
      addSource(currentMediaMetadata) {
        postValue(mediaController.value?.queue)
      }
    }

  override fun getLogTag(): String = "PlayerViewModel"
}