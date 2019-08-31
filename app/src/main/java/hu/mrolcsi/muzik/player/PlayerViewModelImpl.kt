package hu.mrolcsi.muzik.player

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_NONE
import android.widget.Toast
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.OnRepeatTouchListener
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.extensions.secondsToTimeStamp
import hu.mrolcsi.muzik.library.miniplayer.MiniPlayerViewModelImpl
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.isPauseEnabled
import hu.mrolcsi.muzik.service.extensions.media.isPlayEnabled
import hu.mrolcsi.muzik.service.extensions.media.isPlaying
import hu.mrolcsi.muzik.service.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.muzik.service.extensions.media.isSkipToPreviousEnabled
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

class PlayerViewModelImpl @Inject constructor(
  private val context: Context,
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  private val mediaService: MediaService
) : MiniPlayerViewModelImpl(
  observable,
  uiCommandSource,
  navCommandSource,
  themedViewModel,
  mediaService
), PlayerViewModel {

  override val albumArt = MutableLiveData<Drawable>()

  override var elapsedTimeText: String? by boundStringOrNull(BR.elapsedTimeText)
  override var remainingTimeText: String? by boundStringOrNull(BR.remainingTimeText)

  override var shuffleDrawableRes: Int by boundInt(BR.shuffleDrawableRes, R.drawable.ic_shuffle_all)
  override var repeatDrawableRes: Int by boundInt(BR.repeatDrawableRes, R.drawable.ic_repeat_all)

  private var userInitiatedChange = false

  override fun onShuffleClicked() {
    userInitiatedChange = true
    mediaService.toggleShuffle()
  }

  override fun onRepeatClicked() {
    userInitiatedChange = true
    mediaService.toggleRepeat()
  }

  override var isSeekProgressVisible: Boolean by boundBoolean(BR.seekProgressVisible, false)
  override var seekProgressText: String? by boundStringOrNull(BR.seekProgressText)
  private var seekProgress: Int = 0

  override fun onStartTrackingTouch() {
    userInitiatedChange = true
  }

  override fun onSeek(progress: Int, fromUser: Boolean) {
    if (fromUser) {
      seekProgress = progress
      seekProgressText = progress.secondsToTimeStamp()
      isSeekProgressVisible = true
    }
  }

  override fun onStopTrackingTouch() {
    isSeekProgressVisible = false
    userInitiatedChange = false
    mediaService.seekTo(seekProgress * 1000L)
  }

  override val queue = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
  override val currentQueueId = MutableLiveData<Long>()

  override fun getCurrentQueueId(): Long = currentQueueId.value ?: -1

  override fun skipToQueueItem(itemId: Long) {
    mediaService.skipToQueueItem(itemId)
  }

  override val previousTouchListener = OnRepeatTouchListener(
    initialInterval = 500,
    normalInterval = 500,
    onRepeat = { mediaService.rewind(); seekProgressText = elapsedTimeText },
    onDown = { isSeekProgressVisible = true },
    onUp = { isSeekProgressVisible = false }
  )
  override val nextTouchListener = OnRepeatTouchListener(
    initialInterval = 500,
    normalInterval = 500,
    onRepeat = { mediaService.fastForward(); seekProgressText = elapsedTimeText },
    onDown = { isSeekProgressVisible = true },
    onUp = { isSeekProgressVisible = false }
  )

  init {
    mediaService.playbackState
      .map { it.activeQueueItemId }
      .distinctUntilChanged()
      .subscribeBy { currentQueueId.value = it }
      .disposeOnCleared()

    mediaService.shuffleMode
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = {
          when (it) {
            SHUFFLE_MODE_ALL -> shuffleDrawableRes = R.drawable.ic_shuffle_all
            SHUFFLE_MODE_NONE -> shuffleDrawableRes = R.drawable.ic_shuffle_none
          }
          if (userInitiatedChange) {
            userInitiatedChange = false
            sendUiCommand {
              when (it) {
                SHUFFLE_MODE_ALL ->
                  Toast.makeText(this, R.string.player_shuffleEnabled, Toast.LENGTH_SHORT).show()
                SHUFFLE_MODE_NONE ->
                  Toast.makeText(this, R.string.player_shuffleDisabled, Toast.LENGTH_SHORT).show()
              }
            }
          }
        },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    mediaService.repeatMode
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = {
          when (it) {
            REPEAT_MODE_NONE -> repeatDrawableRes = R.drawable.ic_repeat_none
            REPEAT_MODE_ONE -> repeatDrawableRes = R.drawable.ic_repeat_one
            REPEAT_MODE_ALL -> repeatDrawableRes = R.drawable.ic_repeat_all
          }
          if (userInitiatedChange) {
            userInitiatedChange = false
            sendUiCommand {
              when (it) {
                REPEAT_MODE_NONE ->
                  Toast.makeText(this, R.string.player_repeatDisabled, Toast.LENGTH_SHORT).show()
                REPEAT_MODE_ONE ->
                  Toast.makeText(this, R.string.player_repeatOne, Toast.LENGTH_SHORT).show()
                REPEAT_MODE_ALL ->
                  Toast.makeText(this, R.string.player_repeatAll, Toast.LENGTH_SHORT).show()
              }
            }
          }
        },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    mediaService.queue
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { queue.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

  override fun updateState(playbackState: PlaybackStateCompat) {
    val elapsedTime = (playbackState.position / 1000).toInt()
    if (!userInitiatedChange) {
      this.elapsedTime = elapsedTime
    }
    elapsedTimeText = elapsedTime.secondsToTimeStamp()
    remainingTimeText = "-${(duration - elapsedTime).secondsToTimeStamp()}"

    isPreviousEnabled = playbackState.isSkipToPreviousEnabled
    isPlayPauseEnabled = playbackState.isPauseEnabled || playbackState.isPlayEnabled
    isNextEnabled = playbackState.isSkipToNextEnabled

    isPlaying = playbackState.isPlaying
  }

  override fun updateMetadata(metadata: MediaMetadataCompat) {
    super.updateMetadata(metadata)

    remainingTimeText = "-${(duration - elapsedTime).secondsToTimeStamp()}"

    GlideApp.with(context)
      .asDrawable()
      .load(metadata.albumArtUri)
      .onResourceReady { art -> albumArt.value = art }
      .preload()
  }
}