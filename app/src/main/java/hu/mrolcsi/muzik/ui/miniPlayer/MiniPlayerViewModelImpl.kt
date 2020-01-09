package hu.mrolcsi.muzik.ui.miniPlayer

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.core.view.ViewCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import hu.mrolcsi.muzik.MainNavigationDirections
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.manager.media.MediaManager
import hu.mrolcsi.muzik.data.model.media.albumArtUri
import hu.mrolcsi.muzik.data.model.media.artist
import hu.mrolcsi.muzik.data.model.media.duration
import hu.mrolcsi.muzik.data.model.media.isPauseEnabled
import hu.mrolcsi.muzik.data.model.media.isPlayEnabled
import hu.mrolcsi.muzik.data.model.media.isPlaying
import hu.mrolcsi.muzik.data.model.media.isSkipToNextEnabled
import hu.mrolcsi.muzik.data.model.media.isSkipToPreviousEnabled
import hu.mrolcsi.muzik.data.model.media.title
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.concurrent.TimeUnit

open class MiniPlayerViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  MiniPlayerViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaManager: MediaManager by inject()

  override var songTitle: String? by boundStringOrNull(BR.songTitle)
  override var songArtist: String? by boundStringOrNull(BR.songArtist)
  override var coverArtTransitionName: String by boundString(BR.coverArtTransitionName, "coverArt")
  override var coverArtUri = MutableLiveData<Uri>()

  override var duration: Int by boundInt(BR.duration, 0)
  override var elapsedTime: Int by boundInt(BR.elapsedTime, 0)

  override var isPlaying: Boolean by boundBoolean(BR.playing, false)
  override var isPreviousEnabled: Boolean by boundBoolean(BR.previousEnabled, false)
  override var isPlayPauseEnabled: Boolean by boundBoolean(BR.playPauseEnabled, false)
  override var isNextEnabled: Boolean by boundBoolean(BR.nextEnabled, false)

  override fun openPlayer(transitionedView: View) {
    sendNavCommand {
      val transitionName = ViewCompat.getTransitionName(transitionedView)!!
      navigate(
        MainNavigationDirections.actionShowPlayer(),
        FragmentNavigatorExtras(transitionedView to transitionName)
      )
    }
  }

  override fun onPreviousClick() {
    if (elapsedTime > 5) {
      // restart the song
      mediaManager.seekTo(0)
    } else {
      mediaManager.skipToPrevious()
    }
  }

  override fun onPlayPauseClick() {
    mediaManager.playPause()
  }

  override fun onNextClick() {
    mediaManager.skipToNext()
  }

  init {
    mediaManager.playbackState
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { updateState(it) },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    mediaManager.mediaMetadata
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { updateMetadata(it) },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    Observable.interval(500, TimeUnit.MILLISECONDS, Schedulers.single())
      .filter { mediaManager.getCurrentPlaybackState() != null }
      .map { mediaManager.getCurrentPlaybackState()!! }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { updateState(it) },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

  protected open fun updateState(playbackState: PlaybackStateCompat) {
    val elapsedTime = playbackState.position / 1000
    this.elapsedTime = elapsedTime.toInt()

    coverArtTransitionName = "coverArt${playbackState.activeQueueItemId}"

    isPreviousEnabled = playbackState.isSkipToPreviousEnabled
    isPlayPauseEnabled = playbackState.isPauseEnabled || playbackState.isPlayEnabled
    isNextEnabled = playbackState.isSkipToNextEnabled

    isPlaying = playbackState.isPlaying
  }

  protected open fun updateMetadata(metadata: MediaMetadataCompat) {
    coverArtUri.value = metadata.albumArtUri

    duration = (metadata.duration / 1000).toInt()

    songTitle = metadata.title ?: context.getString(R.string.player_noSongLoaded)
    songArtist = metadata.artist
  }
}