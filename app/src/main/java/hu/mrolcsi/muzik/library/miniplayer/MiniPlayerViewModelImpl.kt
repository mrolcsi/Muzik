package hu.mrolcsi.muzik.library.miniplayer

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.core.view.ViewCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.library.LibraryFragmentDirections
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.duration
import hu.mrolcsi.muzik.service.extensions.media.isPauseEnabled
import hu.mrolcsi.muzik.service.extensions.media.isPlayEnabled
import hu.mrolcsi.muzik.service.extensions.media.isPlaying
import hu.mrolcsi.muzik.service.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.muzik.service.extensions.media.isSkipToPreviousEnabled
import hu.mrolcsi.muzik.service.extensions.media.title
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class MiniPlayerViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  private val context: Context,
  private val mediaService: MediaService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  MiniPlayerViewModel {

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
        LibraryFragmentDirections.actionLibraryToPlayer(),
        FragmentNavigatorExtras(transitionedView to transitionName)
      )
    }
  }

  override fun onPreviousClick() {
    if (elapsedTime > 5) {
      // restart the song
      mediaService.seekTo(0)
    } else {
      mediaService.skipToPrevious()
    }
  }

  override fun onPlayPauseClick() {
    mediaService.playPause()
  }

  override fun onNextClick() {
    mediaService.skipToNext()
  }

  init {
    mediaService.playbackState
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { updateState(it) },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    mediaService.mediaMetadata
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { updateMetadata(it) },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    Observable.interval(500, TimeUnit.MILLISECONDS)
      .filter { mediaService.getCurrentPlaybackState() != null }
      .map { mediaService.getCurrentPlaybackState()!! }
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