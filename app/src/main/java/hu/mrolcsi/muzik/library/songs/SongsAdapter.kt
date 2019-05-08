package hu.mrolcsi.muzik.library.songs

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.MuzikGlideModule
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

  override fun getSectionForItem(item: MediaBrowserCompat.MediaItem, sorting: Int): String =
    if (item.isPlayable) super.getSectionForItem(item, sorting) else ""

  open class SongHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private val marqueeDelay by lazy { containerView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong() }

    private var lastTheme: Theme? = null

    open fun bind(item: MediaBrowserCompat.MediaItem, showTrackNumber: Boolean) {
      // Set texts
      tvSongTitle?.run {
        text = item.description.title
        startMarquee(marqueeDelay)
      }
      tvSongArtist?.run {
        if (item.description.subtitle != null) {
          visibility = View.VISIBLE
          text = item.description.subtitle
        } else {
          visibility = View.GONE
        }
        startMarquee(marqueeDelay)
      }

      if (item.description.iconBitmap != null) {
        // Make icon visible
        imgCoverArt.visibility = View.VISIBLE

        // Load icon
        GlideApp.with(imgCoverArt)
          .asDrawable()
          .load(item.description.iconBitmap)
          .addListener(object : MuzikGlideModule.SimpleRequestListener<Drawable> {
            override fun onResourceReady(
              resource: Drawable,
              model: Any?,
              target: Target<Drawable>?,
              dataSource: DataSource?,
              isFirstResource: Boolean
            ): Boolean {
              lastTheme?.secondaryForegroundColor?.let { resource.setTint(it) }
              target?.getSize { width, height ->
                // Add artificial padding using an InsetDrawable
                val drawable = InsetDrawable(resource, width / 3, height / 3, width / 3, height / 3)
                target.onResourceReady(drawable, null)
              }
              return true
            }
          }).into(imgCoverArt)
      } else if (showTrackNumber) {
        // Set track number
        val trackNumber = item.description.trackNumber % 1000
        tvTrackNumber?.visibility = if (showTrackNumber and (trackNumber > 0)) View.VISIBLE else View.GONE
        imgCoverArt?.visibility = if (showTrackNumber) View.GONE else View.VISIBLE
        tvTrackNumber?.text = trackNumber.toString()
      } else {
        // Load album art
        GlideApp.with(imgCoverArt)
          .load(item.description.coverArtUri)
          .into(imgCoverArt)
      }
    }

    open fun applyTheme(theme: Theme) {
      lastTheme = theme
      itemView.background = Theme.getRippleDrawable(theme.secondaryForegroundColor, theme.secondaryBackgroundColor)

      tvSongTitle?.setTextColor(theme.secondaryForegroundColor)
      tvSongArtist?.setTextColor(theme.secondaryForegroundColor)
      tvTrackNumber?.setTextColor(theme.secondaryForegroundColor)
    }

  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "SongsAdapter"
  }
}