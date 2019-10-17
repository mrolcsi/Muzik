package hu.mrolcsi.muzik.library.songs

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.service.extensions.media.artistKey
import hu.mrolcsi.muzik.service.extensions.media.dateAdded
import hu.mrolcsi.muzik.service.extensions.media.isNowPlaying
import hu.mrolcsi.muzik.service.extensions.media.mediaId
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlin.properties.Delegates

class SongsViewModelImpl constructor(
  val context: Context,
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  mediaRepo: MediaRepository,
  private val mediaService: MediaService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  SongsViewModel {

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false

  override val items = MutableLiveData<List<MediaItem>>()
  override var sortingMode: SortingMode by Delegates.observable(SortingMode.SORT_BY_TITLE) { _, old, new ->
    if (old != new) sortingModeSubject.onNext(new)
  }

  private val sortingModeSubject = BehaviorSubject.createDefault(SortingMode.SORT_BY_TITLE)

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun onSongClick(songItem: MediaItem, position: Int) {
    mediaService.setQueueTitle(context.getString(R.string.playlist_allSongs))
    songDescriptions?.let { mediaService.playAll(it, position) }
  }

  init {
    Observables.combineLatest(
      sortingModeSubject,
      mediaRepo.getSongs(),
      mediaService.mediaMetadata
        .distinctUntilChanged { t: MediaMetadataCompat -> t.mediaId }
        .filter { it.mediaId != null }
    )
      .subscribeOn(Schedulers.io())
      .map { (sorting, songs, metadata) -> songs.applyNowPlaying(metadata.mediaId).applySorting(sorting) }
      .doOnNext { songs -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

  private fun List<MediaItem>.applySorting(sortingMode: SortingMode): List<MediaItem> {
    return when (sortingMode) {
      SortingMode.SORT_BY_ARTIST -> sortedBy { it.description.artistKey }
      SortingMode.SORT_BY_TITLE -> sortedBy { it.description.titleKey }
      SortingMode.SORT_BY_DATE -> sortedByDescending { it.description.dateAdded }
    }
  }

}

fun List<MediaItem>.applyNowPlaying(mediaId: String?): List<MediaItem> = this.map {
  it.apply {
    it.description.isNowPlaying = it.mediaId == mediaId
  }
}