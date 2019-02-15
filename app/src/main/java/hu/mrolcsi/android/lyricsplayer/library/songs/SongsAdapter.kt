package hu.mrolcsi.android.lyricsplayer.library.songs

import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R

class SongsAdapter : ListAdapter<MediaBrowserCompat.MediaItem, SongsAdapter.SongHolder>(
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

  var showTrackNumber: Boolean = false

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_song, parent, false)
    return SongHolder(itemView)
  }

  override fun onBindViewHolder(holder: SongHolder, position: Int) {
    val item = getItem(position)

    val trackNumber = item.description.extras?.getInt(MediaStore.Audio.Media.TRACK) ?: 0
    holder.tvTrackNumber?.visibility = if (showTrackNumber and (trackNumber > 0)) View.VISIBLE else View.GONE
    holder.tvTrackNumber?.text = trackNumber.toString()
    holder.tvTitle?.text = item.description.title
    holder.tvArtist?.text = item.description.subtitle

    holder.itemView.setOnClickListener {
      // TODO: Navigate to PlayerActivity passing the song id (path?)
      with(AlertDialog.Builder(holder.itemView.context)) {
        setTitle("Song Info")
        setMessage(item.toString())
        setPositiveButton(android.R.string.ok, null)
        show()
      }
    }
  }

  class SongHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvTrackNumber: TextView? = itemView.findViewById(R.id.tvTrackNumber)
    val tvTitle: TextView? = itemView.findViewById(R.id.tvTitle)
    val tvArtist: TextView? = itemView.findViewById(R.id.tvSubtitle)
  }
}