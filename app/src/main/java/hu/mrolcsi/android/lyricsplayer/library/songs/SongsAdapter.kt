package hu.mrolcsi.android.lyricsplayer.library.songs

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.OnItemClickListener
import hu.mrolcsi.android.lyricsplayer.extensions.media.trackNumber
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager

class SongsAdapter(
  private val onItemClickListener: OnItemClickListener<MediaBrowserCompat.MediaItem, SongsAdapter.SongHolder>
) : ListAdapter<MediaBrowserCompat.MediaItem, SongsAdapter.SongHolder>(
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

    with(holder) {
      // Apply theme
      ThemeManager.currentTheme.value?.let { theme ->
        itemView.background = Theme.getRippleDrawable(theme.darkForegroundColor, theme.darkerBackgroundColor)

        tvTitle?.setTextColor(theme.darkerForegroundColor)
        tvArtist?.setTextColor(ColorUtils.setAlphaComponent(theme.darkerForegroundColor, Theme.INACTIVE_OPACITY))
        tvTrackNumber?.setTextColor(theme.darkerForegroundColor)
      }

      // Set texts
      val trackNumber = item.description.trackNumber ?: 0
      holder.tvTrackNumber?.visibility = if (showTrackNumber and (trackNumber > 0)) View.VISIBLE else View.GONE
      holder.tvTrackNumber?.text = trackNumber.toString()
      holder.tvTitle?.text = item.description.title
      holder.tvArtist?.text = item.description.subtitle

      // Set onClickListener
      holder.itemView.setOnClickListener {
        onItemClickListener.onItemClick(item, holder, position, RecyclerView.NO_ID)
      }
    }
  }

  class SongHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvTrackNumber: TextView? = itemView.findViewById(R.id.tvTrackNumber)
    val tvTitle: TextView? = itemView.findViewById(R.id.tvTitle)
    val tvArtist: TextView? = itemView.findViewById(R.id.tvSubtitle)
  }

  companion object {
    private const val LOG_TAG = "SongsAdapter"
  }
}