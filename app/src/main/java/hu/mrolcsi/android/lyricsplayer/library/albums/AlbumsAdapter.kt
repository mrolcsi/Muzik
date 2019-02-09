package hu.mrolcsi.android.lyricsplayer.library.albums

import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item_2, parent, false)
    return AlbumHolder(itemView)
  }

  override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
    val item = getItem(position)

    holder.tvAlbum?.text = item.description.title
    holder.tvArtist?.text = item.description.extras?.getString(MediaStore.Audio.Albums.ARTIST)

    holder.itemView.setOnClickListener {
      val direction = AlbumsBrowserFragmentDirections.actionAlbumsToSongs(item.mediaId)
      it.findNavController().navigate(direction)
    }
  }

  class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvAlbum: TextView? = itemView.findViewById(android.R.id.text1)
    val tvArtist: TextView? = itemView.findViewById(android.R.id.text2)
  }
}