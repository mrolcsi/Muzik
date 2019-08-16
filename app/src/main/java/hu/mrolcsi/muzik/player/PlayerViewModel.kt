package hu.mrolcsi.muzik.player

import androidx.databinding.Bindable
import hu.mrolcsi.muzik.library.miniplayer.MiniPlayerViewModel

interface PlayerViewModel : MiniPlayerViewModel {

  @get:Bindable
  val elapsedTimeText: String?
  @get:Bindable
  val remainingTimeText: String?

  @get:Bindable
  val shuffleDrawableRes: Int
  @get:Bindable
  val repeatDrawableRes: Int

  //val queue: LiveData<List<MediaSessionCompat.QueueItem>>

  fun onShuffleClicked()
  fun onRepeatClicked()

  @get:Bindable
  val isSeekProgressVisible: Boolean
  @get:Bindable
  val seekProgressText: String?

  fun onStartTrackingTouch()
  fun onSeek(progress: Int, fromUser: Boolean)
  fun onStopTrackingTouch()
}