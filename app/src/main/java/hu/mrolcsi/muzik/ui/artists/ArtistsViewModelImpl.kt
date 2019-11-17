package hu.mrolcsi.muzik.ui.artists

import android.support.v4.media.MediaBrowserCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.library.LibraryFragmentDirections
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.data.model.media.id
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import org.koin.core.inject

class ArtistsViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  ArtistsViewModel,
  KoinComponent {

  private val mediaRepo: MediaRepository by inject()

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
        LibraryFragmentDirections.actionToArtistDetails(item.description.id)
      )
    }
  }
}