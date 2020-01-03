package hu.mrolcsi.muzik.ui.player

import android.net.Uri
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.data.model.theme.Theme
import hu.mrolcsi.muzik.ui.common.OnRepeatTouchListener
import hu.mrolcsi.muzik.ui.miniPlayer.MiniPlayerViewModel

interface PlayerViewModel : MiniPlayerViewModel {

  @get:Bindable
  val noSongLoadedVisible: Boolean

  @get:Bindable
  val elapsedTimeText: String?
  @get:Bindable
  val remainingTimeText: String?

  @get:Bindable
  val shuffleDrawableRes: Int
  @get:Bindable
  val repeatDrawableRes: Int

  fun onShuffleClick()
  fun onRepeatClick()

  @get:Bindable
  val isSeekProgressVisible: Boolean
  @get:Bindable
  val seekProgressText: String?

  fun onStartTrackingTouch()
  fun onSeek(progress: Int, fromUser: Boolean)
  fun onStopTrackingTouch()

  val queueState: LiveData<QueueState>

  fun skipToQueueItem(itemId: Long)

  val previousTouchListener: OnRepeatTouchListener
  val nextTouchListener: OnRepeatTouchListener

  fun onArtistClick(artistId: Long)
  fun onAlbumClick(albumId: Long)
}

data class QueueItem(
  val queueId: Long,
  val artistId: Long?,
  val albumId: Long?,
  val titleText: CharSequence,
  val artistText: CharSequence,
  val albumText: CharSequence,
  val coverArtUri: Uri?,
  val theme: Theme
) {

  val transitionName = "coverArt$queueId"
}

data class QueueState(
  val queue: List<QueueItem>,
  val activeQueueId: Long
)