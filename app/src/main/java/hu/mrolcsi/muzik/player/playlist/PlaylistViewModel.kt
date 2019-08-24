package hu.mrolcsi.muzik.player.playlist

import androidx.databinding.Bindable
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface PlaylistViewModel :
  ListViewModel<PlaylistItem>, ThemedViewModel, UiCommandSource, NavCommandSource {

  @get:Bindable
  val queueTitle: CharSequence

}