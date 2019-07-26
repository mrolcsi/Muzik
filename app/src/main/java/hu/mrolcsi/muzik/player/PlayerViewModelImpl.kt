package hu.mrolcsi.muzik.player

import android.app.Application
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import hu.mrolcsi.muzik.library.SessionViewModelBase
import javax.inject.Inject

class PlayerViewModelImpl @Inject constructor(
  app: Application
) : SessionViewModelBase(app), PlayerViewModel {

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