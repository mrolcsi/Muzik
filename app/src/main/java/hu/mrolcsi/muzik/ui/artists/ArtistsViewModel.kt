package hu.mrolcsi.muzik.ui.artists

import com.l4digital.fastscroll.FastScroller
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource

interface ArtistsViewModel : ListViewModel<ArtistItem>, ThemedViewModel,
  UiCommandSource, NavCommandSource,
  FastScroller.SectionIndexer