package hu.mrolcsi.android.lyricsplayer.library

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ArtistAdapter : ListAdapter<MediaBrowserCompat.MediaItem, ArtistAdapter.ArtistHolder>(
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

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(android.R.layout.simple_list_item_1, parent, false)
    return ArtistHolder(view)
  }

  override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
    val item = getItem(position)

    holder.tvArtist?.text = item.description.title
  }

  class ArtistHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvArtist: TextView? = itemView.findViewById(android.R.id.text1)
  }
}