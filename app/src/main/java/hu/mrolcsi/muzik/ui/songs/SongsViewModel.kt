package hu.mrolcsi.muzik.ui.songs

import com.l4digital.fastscroll.FastScroller
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.library.SortingMode

interface SongsViewModel :
  ListViewModel<SongItem>, ThemedViewModel,
  UiCommandSource, NavCommandSource,
  FastScroller.SectionIndexer {

  var sortingMode: SortingMode

  fun onSongClick(songItem: SongItem, position: Int)
}