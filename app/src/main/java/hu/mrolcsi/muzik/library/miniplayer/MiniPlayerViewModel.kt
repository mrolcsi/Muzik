package hu.mrolcsi.muzik.library.miniplayer

import android.graphics.drawable.Drawable
import android.view.View
import androidx.databinding.Bindable
import androidx.databinding.Observable
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource

interface MiniPlayerViewModel : Observable, UiCommandSource, NavCommandSource {

  @get:Bindable
  val songTitle: String?
  @get:Bindable
  val songArtist: String?
  @get:Bindable
  val albumArt: Drawable?
  @get:Bindable
  val albumArtUri: String?

  @get:Bindable
  val songLength: Int
  @get:Bindable
  val songProgress: Int

  @get:Bindable
  val isPlaying: Boolean

  @get:Bindable
  val isPreviousEnabled: Boolean
  @get:Bindable
  val isPlayPauseEnabled: Boolean
  @get:Bindable
  val isNextEnabled: Boolean

  fun openPlayer(transitionedView: View)
  fun onPreviousClicked()
  fun onPlayPauseClicked()
  fun onNextClicked()
}