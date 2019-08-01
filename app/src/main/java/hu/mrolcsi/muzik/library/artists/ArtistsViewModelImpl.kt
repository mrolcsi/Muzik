package hu.mrolcsi.muzik.library.artists

import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.BR
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsFragmentArgs
import hu.mrolcsi.muzik.media.MediaRepository
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

class ArtistsViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  mediaRepo: MediaRepository
) : DataBindingViewModel(observable, uiCommandSource),
  NavCommandSource by navCommandSource,
  ArtistsViewModel {

  override val progressVisible: Boolean by boundBoolean(BR.progressVisible)
  override val listViewVisible: Boolean by boundBoolean(BR.listViewVisible)
  override val emptyViewVisible: Boolean by boundBoolean(BR.emptyViewVisible)

  override val items = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()

  init {
    mediaRepo.artists
      .takeUntilCleared()
      .doOnError { Log.e("ArtistsViewModel", Log.getStackTraceString(it)) }
      .subscribeBy(
        onNext = { items.value = it },
        onError = { sendUiCommand { showError(it) } }
      ).disposeOnClear()
  }

  override fun onSelect(item: MediaBrowserCompat.MediaItem) {
    sendNavCommand {
      navigate(
        R.id.navigation_artistDetails,
        ArtistDetailsFragmentArgs(item).toBundle()
      )
    }
  }
}