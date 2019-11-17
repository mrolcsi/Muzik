package hu.mrolcsi.muzik.artists

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.databinding.ListItemArtistBinding
import hu.mrolcsi.muzik.service.extensions.media.numberOfAlbums
import hu.mrolcsi.muzik.service.extensions.media.numberOfTracks
import kotlinx.android.synthetic.main.list_item_artist.view.*
import kotlin.properties.Delegates

class ArtistHolder(binding: ListItemArtistBinding) :
  MVVMViewHolder<MediaBrowserCompat.MediaItem>(binding.root) {

  override var model: MediaBrowserCompat.MediaItem? by Delegates.observable(null) { _, _: MediaBrowserCompat.MediaItem?, new: MediaBrowserCompat.MediaItem? ->
    new?.let { bind(it) }
  }

  private fun bind(item: MediaBrowserCompat.MediaItem) {
    itemView.run {
      // Set texts
      tvArtist.text = item.description.title

      val numberOfAlbums = item.description.numberOfAlbums
      val numberOfSongs = item.description.numberOfTracks
      val numberOfAlbumsString = itemView.context.resources.getQuantityString(
        R.plurals.artists_numberOfAlbums,
        numberOfAlbums,
        numberOfAlbums
      )
      val numberOfSongsString = itemView.context.resources.getQuantityString(
        R.plurals.artists_numberOfSongs,
        numberOfSongs,
        numberOfSongs
      )
      tvNumberOfSongs.text = itemView.context.getString(
        R.string.artists_item_subtitle,
        numberOfAlbumsString,
        numberOfSongsString
      )
    }
  }
}