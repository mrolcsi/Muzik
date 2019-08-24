package hu.mrolcsi.muzik.library.albums.details

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource
import hu.mrolcsi.muzik.theme.Theme
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface AlbumDetailsViewModel :
  ListViewModel<MediaItem>, ThemedViewModel, UiCommandSource, NavCommandSource {

  var albumItem: MediaItem?

  val albumDetails: LiveData<MediaItem>

  val albumTheme: LiveData<Theme>

  fun onSongClick(songItem: MediaItem, position: Int)
}