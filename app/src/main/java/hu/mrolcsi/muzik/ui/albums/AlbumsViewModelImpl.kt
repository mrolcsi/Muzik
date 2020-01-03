package hu.mrolcsi.muzik.ui.albums

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.model.media.albumArtUri
import hu.mrolcsi.muzik.data.model.media.albumKey
import hu.mrolcsi.muzik.data.model.media.artistKey
import hu.mrolcsi.muzik.data.model.media.id
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.library.LibraryFragmentDirections
import hu.mrolcsi.muzik.ui.library.SortingMode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.properties.Delegates

class AlbumsViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  AlbumsViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaRepo: MediaRepository by inject()

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false

  override val items = MutableLiveData<List<AlbumItem>>()

  override var sortingMode: SortingMode by Delegates.observable(SortingMode.SORT_BY_TITLE) { _, old, new ->
    if (old != new) sortingModeSubject.onNext(new)
  }

  private var sortingModeSubject = BehaviorSubject.createDefault(SortingMode.SORT_BY_TITLE)

  override fun onAlbumClick(item: AlbumItem, transitionedView: View) {
    val transitionName = ViewCompat.getTransitionName(transitionedView)!!
    sendNavCommand {
      navigate(
        LibraryFragmentDirections.actionToAlbumDetails(item.id, transitionName),
        FragmentNavigatorExtras(transitionedView to transitionName)
      )
    }
  }

  init {
    Observables.combineLatest(
      sortingModeSubject,
      mediaRepo.getAlbums()
    )
      .map { (sortingMode, albums) -> albums.applySorting(sortingMode) }
      .map { it.asAlbumItems(context) }
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

fun List<MediaItem>.asAlbumItems(context: Context) = map { item ->
  AlbumItem(
    item.description.id,
    item.description.title ?: context.getText(R.string.songs_noTitle),
    item.description.subtitle ?: context.getText(R.string.songs_unknownArtist),
    item.description.albumArtUri
  )
}