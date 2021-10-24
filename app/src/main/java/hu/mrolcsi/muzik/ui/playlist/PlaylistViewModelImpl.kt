package hu.mrolcsi.muzik.ui.playlist

import android.content.Context
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.local.playQueue.PlayQueueDao
import hu.mrolcsi.muzik.data.manager.media.MediaBrowserClient
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
  private val mediaBrowserClient: MediaBrowserClient by inject()

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false
  override val items = MutableLiveData<List<PlaylistItem>>()

  override var queueTitle: CharSequence by boundProperty(BR.queueTitle, context.getString(R.string.playlist_title))

  override fun onSelect(item: PlaylistItem) {
    mediaBrowserClient.skipToQueueItem(item.id)
  }

  init {
    Observables.combineLatest(
      playQueueDao.observeQueue(),
      mediaBrowserClient.playbackState.distinctUntilChanged { t: PlaybackStateCompat -> t.activeQueueItemId }
    )
      .observeOn(AndroidSchedulers.mainThread())
      .map { (entries, state) ->
        entries.map {
          PlaylistItem(
            id = it._id,
            mediaId = it.mediaId,
            titleText = it.title ?: context.getString(R.string.songs_noTitle),
            artistText = it.artist ?: context.getString(R.string.songs_unknownArtist),
            durationText = it.duration.millisecondsToTimeStamp(),
            albumArtUri = Uri.parse("content://media/external/audio/albumart/${it.albumId}"),
            isPlaying = it._id == state.activeQueueItemId
          )
        }
      }
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    mediaBrowserClient.queueTitle
      .subscribeBy(
        onNext = { queueTitle = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }
}
