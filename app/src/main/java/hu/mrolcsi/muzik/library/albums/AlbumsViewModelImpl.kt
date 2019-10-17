package hu.mrolcsi.muzik.library.albums

import android.support.v4.media.MediaBrowserCompat.MediaItem
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
import hu.mrolcsi.muzik.service.extensions.media.albumKey
import hu.mrolcsi.muzik.service.extensions.media.artistKey
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.type
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlin.properties.Delegates

class AlbumsViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  mediaRepo: MediaRepository
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  AlbumsViewModel, ThemedViewModel by themedViewModel {

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false

  override val items = MutableLiveData<List<MediaItem>>()

  override var sortingMode: SortingMode by Delegates.observable(SortingMode.SORT_BY_TITLE) { _, old, new ->
    if (old != new) sortingModeSubject.onNext(new)
  }

  private var sortingModeSubject = BehaviorSubject.createDefault(SortingMode.SORT_BY_TITLE)

  override fun onAlbumClick(albumItem: MediaItem, transitionedView: View) {
    if (albumItem.description.type == MediaType.MEDIA_ALBUM) {
      val transitionName = ViewCompat.getTransitionName(transitionedView)!!
      sendNavCommand {
        navigate(
          LibraryPagerFragmentDirections.actionToAlbumDetails(albumItem.description.id, transitionName),
          FragmentNavigatorExtras(transitionedView to transitionName)
        )
      }
    }
  }

  init {
    Observables.combineLatest(
      sortingModeSubject,
      mediaRepo.getAlbums()
    )
      .map { (sortingMode, albums) -> albums.applySorting(sortingMode) }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

  private fun List<MediaItem>.applySorting(sortingMode: SortingMode): List<MediaItem> {
    return when (sortingMode) {
      SortingMode.SORT_BY_ARTIST -> sortedBy { it.description.artistKey }
      SortingMode.SORT_BY_TITLE -> sortedBy { it.description.albumKey }
      SortingMode.SORT_BY_DATE -> throw IllegalArgumentException("Invalid sorting mode for albums!")
    }
  }

}