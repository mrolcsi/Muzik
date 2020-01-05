package hu.mrolcsi.muzik.ui.artists

import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource

interface ArtistsViewModel : ListViewModel<ArtistItem>, ThemedViewModel, UiCommandSource, NavCommandSource {

  fun getSectionText(artistItem: ArtistItem): CharSequence

}