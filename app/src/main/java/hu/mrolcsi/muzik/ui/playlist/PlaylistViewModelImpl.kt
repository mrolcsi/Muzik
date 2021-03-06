package hu.mrolcsi.muzik.ui.playlist

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.local.playQueue.PlayQueueDao
import hu.mrolcsi.muzik.data.manager.media.MediaManager
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.common.extensions.millisecondsToTimeStamp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import org.koin.core.inject

class PlaylistViewModelImpl constructor(
  context: Context,
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  PlaylistViewModel,
  KoinComponent {

  private val playQueueDao: PlayQueueDao by inject()
  private val mediaManager: MediaManager by inject()

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false
  override val items = MutableLiveData<List<PlaylistItem>>()

  override var queueTitle: CharSequence by boundProperty(BR.queueTitle, context.getString(R.string.playlist_title))

  override fun onSelect(item: PlaylistItem) {
    mediaManager.skipToQueueItem(item.id)
  }

  init {
    Observables.combineLatest(
      playQueueDao.fetchQueue(),
      mediaManager.playbackState.distinctUntilChanged { t: PlaybackStateCompat -> t.activeQueueItemId }
    )
      .observeOn(AndroidSchedulers.mainThread())
      .map { (entries, state) ->
        entries.map {
          PlaylistItem(
            it._id,
            it.mediaId,
            it.title ?: context.getString(R.string.songs_noTitle),
            it.artist ?: context.getString(R.string.songs_unknownArtist),
            it.duration.millisecondsToTimeStamp(),
            it._id == state.activeQueueItemId
          )
        }
      }
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    mediaManager.queueTitle
      .subscribeBy(
        onNext = { queueTitle = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

}
