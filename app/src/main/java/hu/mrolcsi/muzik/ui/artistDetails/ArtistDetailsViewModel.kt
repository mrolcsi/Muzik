package hu.mrolcsi.muzik.ui.artistDetails

import android.net.Uri
import android.view.View
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.ui.albums.AlbumItem
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.songs.SongItem

interface ArtistDetailsViewModel : ThemedViewModel, Observable, UiCommandSource,
  NavCommandSource {

  fun setArgument(artistId: Long)

  val artistSongs: LiveData<List<SongItem>>

  val artistAlbums: LiveData<List<AlbumItem>>

  @get:Bindable
  val artistName: String?

  val artistPicture: LiveData<Uri>

  @get:Bindable
  val isAlbumsVisible: Boolean

  fun onAlbumClick(albumItem: AlbumItem, transitionedView: View)

  fun onSongClick(songItem: SongItem, position: Int)

  fun onShuffleAllClick()
}