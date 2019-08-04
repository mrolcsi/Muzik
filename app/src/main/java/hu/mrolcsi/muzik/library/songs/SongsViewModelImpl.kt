package hu.mrolcsi.muzik.library.songs

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.artistKey
import hu.mrolcsi.muzik.service.extensions.media.dateAdded
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import hu.mrolcsi.muzik.service.extensions.media.type
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class SongsViewModelImpl @Inject constructor(
  val context: Context,
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  mediaRepo: MediaRepository,
  private val mediaService: MediaService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  SongsViewModel {

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false

  override val items = MutableLiveData<List<MediaItem>>()
  override var sortingMode: SortingMode by Delegates.observable(SortingMode.SORT_BY_TITLE) { _, old, new ->
    if (old != new) sortingModeSubject.onNext(new)
  }

  private val sortingModeSubject = PublishSubject.create<SortingMode>()

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun onSongClicked(songItem: MediaItem, position: Int) {
    mediaService.setQueueTitle(context.getString(R.string.playlist_allSongs))

    if (songItem.description.type == MediaType.MEDIA_OTHER) {
      songDescriptions?.let { mediaService.playAllShuffled(it) }
    } else {
      songDescriptions?.let { mediaService.playAll(it, position - 1) }
    }
  }

  private val shuffleAllItem = MediaItem(
    MediaDescriptionCompat.Builder()
      .setMediaId("shuffle/all")
      .setTitle(context.getString(R.string.mediaControl_shuffleAll))
      .setIconBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.ic_shuffle))
      .setExtras(bundleOf(MediaType.MEDIA_TYPE_KEY to MediaType.MEDIA_OTHER))
      .build(),
    0
  )

  init {
    Observables.combineLatest(
      sortingModeSubject.startWith(sortingMode),
      mediaRepo.getSongs()
    )
      .map { (sorting, songs) -> songs.applySorting(sorting) }
      .doOnNext { songs -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
      .map { it.toMutableList().apply { add(0, shuffleAllItem) }.toList() }
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnClear()
  }

  private fun List<MediaItem>.applySorting(sortingMode: SortingMode): List<MediaItem> {
    return when (sortingMode) {
      SortingMode.SORT_BY_ARTIST -> sortedBy { it.description.artistKey }
      SortingMode.SORT_BY_TITLE -> sortedBy { it.description.titleKey }
      SortingMode.SORT_BY_DATE -> sortedByDescending { it.description.dateAdded }
    }
  }

}