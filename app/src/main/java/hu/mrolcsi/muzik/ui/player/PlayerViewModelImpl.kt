package hu.mrolcsi.muzik.ui.player

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.media.MediaDescriptionCompat
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
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.manager.media.MediaBrowserClient
import hu.mrolcsi.muzik.data.model.media.album
import hu.mrolcsi.muzik.data.model.media.albumArtUri
import hu.mrolcsi.muzik.data.model.media.albumId
import hu.mrolcsi.muzik.data.model.media.artist
import hu.mrolcsi.muzik.data.model.media.artistId
import hu.mrolcsi.muzik.data.model.media.isPauseEnabled
import hu.mrolcsi.muzik.data.model.media.isPlayEnabled
import hu.mrolcsi.muzik.data.model.media.isPlaying
import hu.mrolcsi.muzik.data.model.media.isSkipToNextEnabled
import hu.mrolcsi.muzik.data.model.media.isSkipToPreviousEnabled
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.common.OnRepeatTouchListener
import hu.mrolcsi.muzik.ui.common.extensions.secondsToTimeStamp
import hu.mrolcsi.muzik.ui.common.glide.GlideApp
import hu.mrolcsi.muzik.ui.common.glide.toSingle
import hu.mrolcsi.muzik.ui.miniPlayer.MiniPlayerViewModelImpl
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.koin.core.inject

class PlayerViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : MiniPlayerViewModelImpl(observable, uiCommandSource, navCommandSource, themedViewModel),
  PlayerViewModel {

  private val context: Context by inject()
  private val mediaBrowserClient: MediaBrowserClient by inject()

  override var noSongLoadedVisible: Boolean by boundBoolean(BR.noSongLoadedVisible)

  override var elapsedTimeText: String? by boundStringOrNull(BR.elapsedTimeText)
  override var remainingTimeText: String? by boundStringOrNull(BR.remainingTimeText)

  override var shuffleDrawableRes: Int by boundInt(BR.shuffleDrawableRes, R.drawable.ic_shuffle_all)
  override var repeatDrawableRes: Int by boundInt(BR.repeatDrawableRes, R.drawable.ic_repeat_all)

  private var userInitiatedChange = false

  private val emptyQueueItem = MediaSessionCompat.QueueItem(
    MediaDescriptionCompat.Builder()
      .setTitle(context.getString(R.string.player_noSongLoaded))
      .build(),
    0
  )

  override fun onShuffleClick() {
    userInitiatedChange = true
    mediaBrowserClient.toggleShuffle()
  }

  override fun onRepeatClick() {
    userInitiatedChange = true
    mediaBrowserClient.toggleRepeat()
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
    mediaBrowserClient.seekTo(seekProgress * 1000L)
  }

  override val queueState = MutableLiveData<QueueState>()

  override fun skipToQueueItem(itemId: Long) {
    mediaBrowserClient.skipToQueueItem(itemId)
  }

  override val previousTouchListener = OnRepeatTouchListener(
    initialInterval = 500,
    normalInterval = 500,
    onRepeat = { mediaBrowserClient.rewind(); seekProgressText = elapsedTimeText },
    onDown = { isSeekProgressVisible = true },
    onUp = { isSeekProgressVisible = false }
  )
  override val nextTouchListener = OnRepeatTouchListener(
    initialInterval = 500,
    normalInterval = 500,
    onRepeat = { mediaBrowserClient.fastForward(); seekProgressText = elapsedTimeText },
    onDown = { isSeekProgressVisible = true },
    onUp = { isSeekProgressVisible = false }
  )

  override fun onArtistClick(artistId: Long) {
    sendNavCommand {
      navigate(PlayerDialogFragmentDirections.actionToArtistDetails(artistId))
    }
  }

  override fun onAlbumClick(albumId: Long) {
    sendNavCommand {
      navigate(PlayerDialogFragmentDirections.actionToAlbumDetails(albumId))
    }
  }

  init {
    mediaBrowserClient.shuffleMode
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

    mediaBrowserClient.repeatMode
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

    Observables.combineLatest(
      mediaBrowserClient.queue
        .startWith(listOf(emptyQueueItem))
        .distinctUntilChanged { old, new -> old.map { it.queueId } == new.map { it.queueId } }
        .switchMapSingle { queueItems -> queueItems.createThemes() },
      mediaBrowserClient.playbackState
        .map { it.activeQueueItemId }
        .distinctUntilChanged()
        .doOnNext { noSongLoadedVisible = it < 0 }
    )
      .map { (queue, activeQueueId) -> QueueState(queue, activeQueueId) }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { queueState.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

  private fun List<MediaSessionCompat.QueueItem>.createThemes() =
    Observable.fromIterable(this)
      .subscribeOn(Schedulers.computation())
      .concatMapSingle { item ->
        // Get coverArt
        GlideApp.with(context)
          .asBitmap()
          .load(item.description.albumArtUri)
          .override(Target.SIZE_ORIGINAL)
          .toSingle()
          .onErrorReturn { BitmapFactory.decodeResource(context.resources, R.drawable.placeholder_cover_art) }
          .map { item to it }
      }
      .concatMapSingle { (item, coverArt) ->
        // Generate theme from coverArt
        themeService
          .createTheme(coverArt)
          .map { theme ->
            QueueItem(
              item.queueId,
              item.description.artistId,
              item.description.albumId,
              item.description.title ?: context.getString(R.string.songs_noTitle),
              item.description.artist ?: context.getString(R.string.songs_unknownArtist),
              item.description.album ?: context.getString(R.string.songs_unknownAlbum),
              item.description.albumArtUri,
              theme
            )
          }
      }.toList()

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
  }
}