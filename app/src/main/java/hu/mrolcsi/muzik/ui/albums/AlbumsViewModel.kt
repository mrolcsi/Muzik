package hu.mrolcsi.muzik.ui.albums

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.library.SortingMode
import hu.mrolcsi.muzik.ui.base.ThemedViewModel

interface AlbumsViewModel :
  ListViewModel<MediaBrowserCompat.MediaItem>, ThemedViewModel, UiCommandSource,
  NavCommandSource {

  var sortingMode: SortingMode

  fun onAlbumClick(albumItem: MediaBrowserCompat.MediaItem, transitionedView: View)

}