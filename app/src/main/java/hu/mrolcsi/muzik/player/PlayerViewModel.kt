package hu.mrolcsi.muzik.player

import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaSessionCompat
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.common.OnRepeatTouchListener
import hu.mrolcsi.muzik.library.miniplayer.MiniPlayerViewModel

interface PlayerViewModel : MiniPlayerViewModel {

  val albumArt: LiveData<Drawable>

  @get:Bindable
  val elapsedTimeText: String?
  @get:Bindable
  val remainingTimeText: String?

  @get:Bindable
  val shuffleDrawableRes: Int
  @get:Bindable
  val repeatDrawableRes: Int

  fun onShuffleClicked()
  fun onRepeatClicked()

  @get:Bindable
  val isSeekProgressVisible: Boolean
  @get:Bindable
  val seekProgressText: String?

  fun onStartTrackingTouch()
  fun onSeek(progress: Int, fromUser: Boolean)
  fun onStopTrackingTouch()

  val queue: LiveData<List<MediaSessionCompat.QueueItem>>
  fun getCurrentQueueId(): Long

  fun skipToQueueItem(itemId: Long)

  val previousTouchListener: OnRepeatTouchListener
  val nextTouchListener: OnRepeatTouchListener
}