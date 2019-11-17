package hu.mrolcsi.muzik.ui.songs

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.library.SortingMode
import hu.mrolcsi.muzik.ui.base.ThemedViewModel

interface SongsViewModel :
  ListViewModel<MediaBrowserCompat.MediaItem>, ThemedViewModel, UiCommandSource,
  NavCommandSource {

  var sortingMode: SortingMode

  fun onSongClick(songItem: MediaBrowserCompat.MediaItem, position: Int)

}