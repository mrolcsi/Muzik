package hu.mrolcsi.muzik.ui.playlist

import androidx.databinding.Bindable
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.base.ThemedViewModel

interface PlaylistViewModel :
  ListViewModel<PlaylistItem>, ThemedViewModel, UiCommandSource,
  NavCommandSource {

  @get:Bindable
  val queueTitle: CharSequence

}