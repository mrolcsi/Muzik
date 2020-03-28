package hu.mrolcsi.muzik.ui.playlist

import android.net.Uri
import androidx.databinding.Bindable
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource

interface PlaylistViewModel :
  ListViewModel<PlaylistItem>, ThemedViewModel, UiCommandSource,
  NavCommandSource {

  @get:Bindable
  val queueTitle: CharSequence
}

data class PlaylistItem(
  val id: Long,
  val mediaId: Long,
  val titleText: String,
  val artistText: String,
  val durationText: String,
  val albumArtUri: Uri?,
  val isPlaying: Boolean
)