package hu.mrolcsi.muzik.library.songs

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.DiffCallbackRepository
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.service.extensions.media.trackNumber
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager

class SongsAdapter(
  private val onItemClickListener: OnItemClickListener<MediaBrowserCompat.MediaItem, SongHolder>
) : ListAdapter<MediaBrowserCompat.MediaItem, SongsAdapter.SongHolder>(
  DiffCallbackRepository.mediaItemCallback
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
      ThemeManager.getInstance(holder.itemView.context).currentTheme.value?.let { theme ->
        itemView.background = Theme.getRippleDrawable(theme.secondaryForegroundColor, theme.tertiaryBackgroundColor)

        tvTitle?.setTextColor(theme.tertiaryForegroundColor)
        tvArtist?.setTextColor(ColorUtils.setAlphaComponent(theme.tertiaryForegroundColor, Theme.SUBTITLE_OPACITY))
        tvTrackNumber?.setTextColor(theme.tertiaryForegroundColor)
      }

      // Set texts
      val trackNumber = item.description.trackNumber
      holder.tvTrackNumber?.visibility = if (showTrackNumber and (trackNumber > 0)) View.VISIBLE else View.GONE
      holder.tvTrackNumber?.text = trackNumber.toString()
      holder.tvTitle?.text = item.description.title
      holder.tvArtist?.text = item.description.subtitle

      // Set onClickListener
      holder.itemView.setOnClickListener {
        onItemClickListener.onItemClick(item, holder, position, getItemId(position))
      }
    }
  }

  class SongHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvTrackNumber: TextView? = itemView.findViewById(R.id.tvTrackNumber)
    val tvTitle: TextView? = itemView.findViewById(R.id.tvTitle)
    val tvArtist: TextView? = itemView.findViewById(R.id.tvSubtitle)
  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "SongsAdapter"
  }
}