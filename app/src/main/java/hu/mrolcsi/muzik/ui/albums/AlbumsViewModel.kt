package hu.mrolcsi.muzik.ui.albums

import android.view.View
import com.l4digital.fastscroll.FastScroller
import hu.mrolcsi.muzik.ui.albumDetails.AlbumDetailItem
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.library.SortingMode

interface AlbumsViewModel :
  ListViewModel<AlbumItem>, ThemedViewModel,
  UiCommandSource, NavCommandSource,
  FastScroller.SectionIndexer {

  var sortingMode: SortingMode

  fun onAlbumClick(item: AlbumItem, transitionedView: View)
}

data class DiscNumberItem(
  override val id: Long,
  val discNumberText: CharSequence
) : AlbumDetailItem