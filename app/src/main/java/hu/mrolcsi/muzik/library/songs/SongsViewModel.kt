package hu.mrolcsi.muzik.library.songs

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface SongsViewModel :
  ListViewModel<MediaBrowserCompat.MediaItem>, ThemedViewModel, UiCommandSource, NavCommandSource {

  var sortingMode: SortingMode

  fun onSongClick(songItem: MediaBrowserCompat.MediaItem, position: Int)

}