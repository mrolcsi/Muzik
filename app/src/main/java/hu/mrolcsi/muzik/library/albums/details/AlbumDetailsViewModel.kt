package hu.mrolcsi.muzik.library.albums.details

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource
import hu.mrolcsi.muzik.theme.Theme
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface AlbumDetailsViewModel :
  ListViewModel<MediaItem>, ThemedViewModel, UiCommandSource, NavCommandSource {

  @get:Bindable val albumTitleText: String?
  @get:Bindable val artistText: String?
  @get:Bindable val yearText: String?
  @get:Bindable val numberOfSongsText: String?

  fun setArgument(albumId: Long)

  val albumItem: LiveData<MediaItem>

  val albumTheme: LiveData<Theme>

  fun onSongClick(songItem: MediaItem, position: Int)

  fun onShuffleAllClick()
}