package hu.mrolcsi.muzik.library.songs

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.DiffCallbackRepository
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import hu.mrolcsi.muzik.service.extensions.media.trackNumber
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_song.*

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

        tvSongTitle?.setTextColor(theme.tertiaryForegroundColor)
        tvSongArtist?.setTextColor(theme.tertiaryForegroundColor)
        tvTrackNumber?.setTextColor(theme.tertiaryForegroundColor)
      }

      // Set onClickListener
      holder.itemView.setOnClickListener {
        onItemClickListener.onItemClick(item, holder, position, getItemId(position))
      }

      bind(item, showTrackNumber)
    }
  }

  class SongHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: MediaBrowserCompat.MediaItem, showTrackNumber: Boolean) {
      // Set texts
      tvSongTitle?.text = item.description.title
      tvSongArtist?.text = item.description.subtitle

      // Load album art
      GlideApp.with(imgCoverArt)
        .asBitmap()
        .load(item.description.coverArtUri)
        .into(imgCoverArt)

      // Set track number
      val trackNumber = item.description.trackNumber
      tvTrackNumber?.visibility = if (showTrackNumber and (trackNumber > 0)) View.VISIBLE else View.GONE
      imgCoverArt?.visibility = if (showTrackNumber) View.GONE else View.VISIBLE
      tvTrackNumber?.text = trackNumber.toString()
    }

  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "SongsAdapter"
  }
}