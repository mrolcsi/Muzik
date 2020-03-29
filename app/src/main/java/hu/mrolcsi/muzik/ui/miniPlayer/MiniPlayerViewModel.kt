package hu.mrolcsi.muzik.ui.miniPlayer

import android.net.Uri
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource

interface MiniPlayerViewModel : ThemedViewModel, Observable, UiCommandSource,
  NavCommandSource {

  @get:Bindable
  val songTitle: String?
  @get:Bindable
  val songArtist: String?
  @get:Bindable
  val coverArtTransitionName: String

  val coverArtUri: LiveData<Uri>

  @get:Bindable
  val duration: Int
  @get:Bindable
  val elapsedTime: Int

  @get:Bindable
  val isPlaying: Boolean

  @get:Bindable
  val isPreviousEnabled: Boolean
  @get:Bindable
  val isPlayPauseEnabled: Boolean
  @get:Bindable
  val isNextEnabled: Boolean

  fun openPlayer()
  fun onPreviousClick()
  fun onPlayPauseClick()
  fun onNextClick()
}