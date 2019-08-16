package hu.mrolcsi.muzik.library.miniplayer

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.core.view.ViewCompat
import androidx.navigation.fragment.FragmentNavigatorExtras
import hu.mrolcsi.muzik.BR
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.player.PlayerFragmentArgs
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.duration
import hu.mrolcsi.muzik.service.extensions.media.isPauseEnabled
import hu.mrolcsi.muzik.service.extensions.media.isPlayEnabled
import hu.mrolcsi.muzik.service.extensions.media.isPlaying
import hu.mrolcsi.muzik.service.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.muzik.service.extensions.media.isSkipToPreviousEnabled
import hu.mrolcsi.muzik.service.extensions.media.title
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

open class MiniPlayerViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  private val mediaService: MediaService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  MiniPlayerViewModel {

  override var songTitle: String? by boundStringOrNull(BR.songTitle)
  override var songArtist: String? by boundStringOrNull(BR.songArtist)
  override var albumArtUri: String? by boundStringOrNull(BR.albumArtUri, null)

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
        R.id.navPlayer,
        PlayerFragmentArgs(transitionName).toBundle(),
        null,
        FragmentNavigatorExtras(transitionedView to transitionName)
      )
    }
  }

  override fun onPreviousClicked() {
    if (elapsedTime > 5) {
      // restart the song
      mediaService.seekTo(0)
    } else {
      mediaService.skipToPrevious()
    }
  }

  override fun onPlayPauseClicked() {
    mediaService.playPause()
  }

  override fun onNextClicked() {
    mediaService.skipToNext()
  }

  init {
    mediaService.playbackState
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy { updateControls(it) }
      .disposeOnCleared()

    mediaService.metadata
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy { updateMetadata(it) }
      .disposeOnCleared()
  }

  protected open fun updateControls(playbackState: PlaybackStateCompat) {
    // Update progress
    val elapsedTime = playbackState.position / 1000
    this.elapsedTime = elapsedTime.toInt()

    isPreviousEnabled = playbackState.isSkipToPreviousEnabled
    isPlayPauseEnabled = playbackState.isPauseEnabled || playbackState.isPlayEnabled
    isNextEnabled = playbackState.isSkipToNextEnabled

    isPlaying = playbackState.isPlaying
  }

  protected open fun updateMetadata(metadata: MediaMetadataCompat) {
    albumArtUri = metadata.albumArtUri?.toString()

    duration = (metadata.duration / 1000).toInt()

    songTitle = metadata.title
    songArtist = metadata.artist
  }
}