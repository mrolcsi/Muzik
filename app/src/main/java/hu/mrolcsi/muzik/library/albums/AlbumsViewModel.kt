package hu.mrolcsi.muzik.library.albums

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource
import hu.mrolcsi.muzik.library.SortingMode

interface AlbumsViewModel : ListViewModel<MediaBrowserCompat.MediaItem>, UiCommandSource, NavCommandSource {

  var sortingMode: SortingMode

  fun onAlbumClicked(albumItem: MediaBrowserCompat.MediaItem, vararg transitionedView: View)

}