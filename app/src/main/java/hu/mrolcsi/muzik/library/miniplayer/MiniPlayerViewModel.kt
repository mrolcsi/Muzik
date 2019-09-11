package hu.mrolcsi.muzik.library.miniplayer

import android.view.View
import androidx.databinding.Bindable
import androidx.databinding.Observable
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface MiniPlayerViewModel : ThemedViewModel, Observable, UiCommandSource, NavCommandSource {

  @get:Bindable
  val songTitle: String?
  @get:Bindable
  val songArtist: String?
  @get:Bindable
  val albumArtUri: String?

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

  fun openPlayer(transitionedView: View)
  fun onPreviousClick()
  fun onPlayPauseClick()
  fun onNextClick()
}