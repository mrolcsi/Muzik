package hu.mrolcsi.muzik.library.artists

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.BR
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.library.pager.LibraryPagerFragmentDirections
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

class ArtistsViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  mediaRepo: MediaRepository
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  ArtistsViewModel {

  override var progressVisible: Boolean by boundBoolean(BR.progressVisible)
  override var listViewVisible: Boolean by boundBoolean(BR.listViewVisible)
  override var emptyViewVisible: Boolean by boundBoolean(BR.emptyViewVisible)

  override val items = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()

  init {
    mediaRepo.getArtists()
      .doOnSubscribe { progressVisible = true }
      .doOnNext { progressVisible = false }
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

  override fun onSelect(item: MediaBrowserCompat.MediaItem) {
    sendNavCommand {
      navigate(
        LibraryPagerFragmentDirections.actionToArtistDetails(item)
      )
    }
  }
}