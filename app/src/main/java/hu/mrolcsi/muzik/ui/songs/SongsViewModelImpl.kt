package hu.mrolcsi.muzik.ui.songs

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.library.SortingMode
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.data.manager.media.MediaManager
import hu.mrolcsi.muzik.data.model.media.artistKey
import hu.mrolcsi.muzik.data.model.media.dateAdded
import hu.mrolcsi.muzik.data.model.media.isNowPlaying
import hu.mrolcsi.muzik.data.model.media.mediaId
import hu.mrolcsi.muzik.data.model.media.titleKey
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.properties.Delegates

class SongsViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  SongsViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaRepo: MediaRepository by inject()
  private val mediaManager: MediaManager by inject()

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
    mediaManager.setQueueTitle(context.getString(R.string.playlist_allSongs))
    songDescriptions?.let { mediaManager.playAll(it, position) }
  }

  init {
    Observables.combineLatest(
      sortingModeSubject,
      mediaRepo.getSongs(),
      mediaManager.mediaMetadata
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