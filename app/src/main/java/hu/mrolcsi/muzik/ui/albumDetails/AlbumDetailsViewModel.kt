package hu.mrolcsi.muzik.ui.albumDetails

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.data.model.theme.Theme
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.songs.SongItem

interface AlbumDetailsViewModel :
  ListViewModel<AlbumDetailItem>, ThemedViewModel, UiCommandSource,
  NavCommandSource {

  @get:Bindable val albumTitleText: String?
  @get:Bindable val artistText: String?
  @get:Bindable val yearText: String?
  @get:Bindable val numberOfSongsText: String?

  fun setArgument(albumId: Long)

  val albumItem: LiveData<MediaItem>

  val albumTheme: LiveData<Theme>

  fun onSongClick(songItem: SongItem)

  fun onShuffleAllClick()
}

interface AlbumDetailItem {
  val id: Long
}