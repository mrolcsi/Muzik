package hu.mrolcsi.muzik.ui.albums

import android.view.View
import hu.mrolcsi.muzik.ui.albumDetails.AlbumDetailItem
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.library.SortingMode

interface AlbumsViewModel :
  ListViewModel<AlbumItem>, ThemedViewModel, UiCommandSource,
  NavCommandSource {

  var sortingMode: SortingMode

  fun onAlbumClick(item: AlbumItem, transitionedView: View)

  fun getSectionText(item: AlbumItem): CharSequence

}

data class DiscNumberItem(
  override val id: Long,
  val discNumberText: CharSequence
) : AlbumDetailItem