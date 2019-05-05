package hu.mrolcsi.muzik.library.songs

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import hu.mrolcsi.muzik.service.extensions.media.trackNumber
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_song.*

open class SongsAdapter(
  context: Context,
  protected val onItemClickListener: OnItemClickListener<MediaBrowserCompat.MediaItem, SongHolder>? = null
) : MediaItemListAdapter<SongsAdapter.SongHolder>(context) {

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
        applyTheme(theme)
      }

      // Set onClickListener
      onItemClickListener?.run {
        holder.itemView.setOnClickListener {
          this.onItemClick(item, holder, position, getItemId(position))
        }
      }

      bind(item, showTrackNumber)
    }
  }

  open class SongHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private val marqueeDelay by lazy { containerView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong() }

    open fun bind(item: MediaBrowserCompat.MediaItem, showTrackNumber: Boolean) {

      // Set texts
      tvSongTitle?.run {
        text = item.description.title
        startMarquee(marqueeDelay)
      }
      tvSongArtist?.run {
        text = item.description.subtitle
        startMarquee(marqueeDelay)
      }

      // Load album art
      GlideApp.with(imgCoverArt)
        .asBitmap()
        .load(item.description.coverArtUri)
        .into(imgCoverArt)

      // Set track number
      val trackNumber = item.description.trackNumber % 1000
      tvTrackNumber?.visibility = if (showTrackNumber and (trackNumber > 0)) View.VISIBLE else View.GONE
      imgCoverArt?.visibility = if (showTrackNumber) View.GONE else View.VISIBLE
      tvTrackNumber?.text = trackNumber.toString()
    }

    open fun applyTheme(theme: Theme) {
      itemView.background = Theme.getRippleDrawable(theme.tertiaryForegroundColor, theme.tertiaryBackgroundColor)

      tvSongTitle?.setTextColor(theme.tertiaryForegroundColor)
      tvSongArtist?.setTextColor(theme.tertiaryForegroundColor)
      tvTrackNumber?.setTextColor(theme.tertiaryForegroundColor)
    }

  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "SongsAdapter"
  }
}