package hu.mrolcsi.muzik.library.artists

import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.media.MediaRepository
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

class ArtistsViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  mediaRepo: MediaRepository
) : DataBindingViewModel(observable, uiCommandSource), ArtistsViewModel {

  override val artists = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()

  init {
    mediaRepo.artists
      .takeUntilCleared()
      .doOnError { Log.e("ArtistsViewModel", Log.getStackTraceString(it)) }
      .subscribeBy(
        onNext = { artists.value = it },
        onError = { showError(it) }
      ).disposeOnClear()
  }
}