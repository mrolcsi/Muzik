package hu.mrolcsi.muzik.player.playlist

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.database.playqueue.daos.PlayQueueDao
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy

class PlaylistViewModelImpl constructor(
  context: Context,
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  playQueueDao: PlayQueueDao,
  private val mediaService: MediaService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  PlaylistViewModel {

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false
  override val items = MutableLiveData<List<PlaylistItem>>()

  override var queueTitle: CharSequence by boundProperty(BR.queueTitle, context.getString(R.string.playlist_title))

  override fun onSelect(item: PlaylistItem) {
    mediaService.skipToQueueItem(item.entry._id)
  }

  init {
    Observables.combineLatest(
      playQueueDao.fetchQueue(),
      mediaService.playbackState.distinctUntilChanged { t: PlaybackStateCompat -> t.activeQueueItemId }
    )
      .observeOn(AndroidSchedulers.mainThread())
      .map { (entries, state) -> entries.map { PlaylistItem(it, it._id == state.activeQueueItemId) } }
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    mediaService.queueTitle
      .subscribeBy(
        onNext = { queueTitle = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

}

data class PlaylistItem(
  val entry: PlayQueueEntry,
  val isPlaying: Boolean
) {

  companion object {
    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PlaylistItem>() {
      override fun areItemsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem) =
        oldItem.entry._id == newItem.entry._id

      override fun areContentsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem) = oldItem == newItem
    }
  }
}
