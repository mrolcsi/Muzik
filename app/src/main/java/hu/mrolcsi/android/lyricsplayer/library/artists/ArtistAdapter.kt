package hu.mrolcsi.android.lyricsplayer.library.artists

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
    val view = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item_2, parent, false)
    return ArtistHolder(view)
  }

  override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
    val item = getItem(position)

    holder.tvArtist?.text = item.description.title
    val numberOfAlbums = item.description.extras?.getInt(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS) ?: 0
    val numberOfTracks = item.description.extras?.getInt(MediaStore.Audio.Artists.NUMBER_OF_TRACKS) ?: 0
    // TODO: quantity string -> "5 albums, 24 tracks"
    holder.tvNumOfSongs?.text = "$numberOfAlbums albums, $numberOfTracks tracks"

    holder.itemView.setOnClickListener {
      val direction = ArtistsFragmentDirections.actionArtistsToAlbums(item.description.title.toString())
      it.findNavController().navigate(direction)
    }
  }

  class ArtistHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvArtist: TextView? = itemView.findViewById(android.R.id.text1)
    val tvNumOfSongs: TextView? = itemView.findViewById(android.R.id.text2)
  }
}