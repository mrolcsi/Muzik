package hu.mrolcsi.muzik.library.albums

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.library.pager.LibraryPagerFragmentDirections
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.artistKey
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import hu.mrolcsi.muzik.service.extensions.media.type
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class AlbumsViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  mediaRepo: MediaRepository
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  AlbumsViewModel {

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false

  override val items = MutableLiveData<List<MediaItem>>()

  override var sortingMode: SortingMode by Delegates.observable(SortingMode.SORT_BY_TITLE) { _, old, new ->
    if (old != new) sortingModeSubject.onNext(new)
  }

  private var sortingModeSubject = PublishSubject.create<SortingMode>()

  override fun onAlbumClicked(albumItem: MediaItem, vararg transitionedView: View) {
    if (albumItem.description.type == MediaType.MEDIA_ALBUM) {
      sendNavCommand {
        navigate(
          LibraryPagerFragmentDirections.actionToAlbumDetails(albumItem),
          FragmentNavigatorExtras(*transitionedView.map { it to ViewCompat.getTransitionName(it)!! }.toTypedArray())
        )
      }
    }
  }

  init {
    Observables.combineLatest(
      sortingModeSubject
        .startWith(sortingMode)
        .doOnNext { Log.v("AlbumsViewModel", "Got sorting mode: $it") },
      mediaRepo.getAlbums()
        .doOnNext { Log.v("AlbumsViewModel", "Got albums: $it") }
    )
      .subscribeOn(Schedulers.io())
      .doOnNext { (sortingMode, albums) -> albums.applySorting(sortingMode) }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { (_, albums) -> items.value = albums },
        onError = { showError(this, it) }
      ).disposeOnClear()
  }

  private fun List<MediaItem>.applySorting(sortingMode: SortingMode): List<MediaItem> {
    return when (sortingMode) {
      SortingMode.SORT_BY_ARTIST -> sortedBy { it.description.artistKey }
      SortingMode.SORT_BY_TITLE -> sortedBy { it.description.titleKey }
      SortingMode.SORT_BY_DATE -> throw IllegalArgumentException("Invalid sorting mode for albums!")
    }
  }

}