package hu.mrolcsi.muzik.player

import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import hu.mrolcsi.muzik.common.view.OnRepeatTouchListener
import hu.mrolcsi.muzik.library.miniplayer.MiniPlayerViewModel
import hu.mrolcsi.muzik.theme.Theme

interface PlayerViewModel : MiniPlayerViewModel {

  val liveAlbumArtUri: LiveData<Uri>

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

  val queue: LiveData<List<ThemedQueueItem>>
  val currentQueueId: LiveData<Long>
  fun getCurrentQueueId(): Long

  fun skipToQueueItem(itemId: Long)

  val previousTouchListener: OnRepeatTouchListener
  val nextTouchListener: OnRepeatTouchListener
}

data class ThemedQueueItem(
  val queueItem: MediaSessionCompat.QueueItem,
  val theme: Theme
) {

  companion object DiffCallback : DiffUtil.ItemCallback<ThemedQueueItem>() {

    override fun areItemsTheSame(oldItem: ThemedQueueItem, newItem: ThemedQueueItem) =
      oldItem.queueItem.queueId == newItem.queueItem.queueId

    override fun areContentsTheSame(oldItem: ThemedQueueItem, newItem: ThemedQueueItem) =
      oldItem == newItem

  }
}