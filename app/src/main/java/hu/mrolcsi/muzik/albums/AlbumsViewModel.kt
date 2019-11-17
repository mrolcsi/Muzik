package hu.mrolcsi.muzik.albums

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface AlbumsViewModel :
  ListViewModel<MediaBrowserCompat.MediaItem>, ThemedViewModel, UiCommandSource, NavCommandSource {

  var sortingMode: SortingMode

  fun onAlbumClick(albumItem: MediaBrowserCompat.MediaItem, transitionedView: View)

}