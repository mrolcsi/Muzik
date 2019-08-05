package hu.mrolcsi.muzik.library.albums.details

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource

interface AlbumDetailsViewModel : ListViewModel<MediaItem>, UiCommandSource, NavCommandSource {

  var albumItem: MediaItem?

  val albumDetails: LiveData<MediaItem>

  fun onSongClick(songItem: MediaItem, position: Int)
}