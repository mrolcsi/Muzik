package hu.mrolcsi.muzik.library.artists.details

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource

interface ArtistDetailsViewModel : UiCommandSource, NavCommandSource {

  var artistItem: MediaBrowserCompat.MediaItem?

  val artistSongs: LiveData<List<MediaBrowserCompat.MediaItem>>

  val artistAlbums: LiveData<List<MediaBrowserCompat.MediaItem>>

  val artistPicture: LiveData<Uri>

  fun onAlbumClick(albumItem: MediaBrowserCompat.MediaItem, vararg transitionedView: View)

  fun onSongClick(songItem: MediaBrowserCompat.MediaItem, position: Int)
}