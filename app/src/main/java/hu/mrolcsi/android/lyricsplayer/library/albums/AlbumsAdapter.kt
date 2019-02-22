package hu.mrolcsi.android.lyricsplayer.library.albums

import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.R

class AlbumsAdapter : ListAdapter<MediaBrowserCompat.MediaItem, AlbumsAdapter.AlbumHolder>(
  object : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
    override fun areItemsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      return oldItem == newItem
    }

    override fun areContentsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      return oldItem.description.mediaId == newItem.description.mediaId
    }
  }
) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_artist, parent, false)
    return AlbumHolder(itemView)
  }

  override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
    val item = getItem(position)

    holder.tvAlbum?.text = item.description.title
    holder.tvArtist?.text = item.description.subtitle

    if (item.mediaId == MEDIA_ID_ALL_SONGS) {
      holder.itemView.setOnClickListener {
        val direction = AlbumsFragmentDirections.actionAlbumsToSongs(
          item.description.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST_KEY),
          item.description.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST),
          null,
          null
        )
        it.findNavController().navigate(direction)
      }
    } else {
      holder.itemView.setOnClickListener {
        with(it.findNavController()) {
          Log.d(LOG_TAG, "Current Destination = $currentDestination")
//          when (currentDestination?.id) {
//            R.id.navigation_albums, R.id.navigation_albumsByArtist -> {
          try {
            val direction = AlbumsFragmentDirections.actionAlbumsToSongs(
              null,
              null,
              item.mediaId,
              item.description.title.toString()
            )
            navigate(direction)
          } catch (e: IllegalArgumentException) {
            Toast.makeText(it.context, "Lost navigation.", Toast.LENGTH_SHORT).show()
          }
//            }
//          }
        }
      }
    }
  }

  class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvAlbum: TextView? = itemView.findViewById(R.id.tvTitle)
    val tvArtist: TextView? = itemView.findViewById(R.id.tvSubtitle)
  }

  companion object {
    private const val LOG_TAG = "AlbumsAdapter"

    const val MEDIA_ID_ALL_SONGS = BuildConfig.APPLICATION_ID + ".ALL_SONGS"
  }
}