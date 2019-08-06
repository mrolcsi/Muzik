package hu.mrolcsi.muzik.library.songs

import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.MuzikGlideModule
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import hu.mrolcsi.muzik.service.extensions.media.trackNumber
import hu.mrolcsi.muzik.service.theme.Theme
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_song.*
import kotlin.properties.Delegates

open class SongHolder(override val containerView: View, private val showTrackNumber: Boolean) :
  MVVMViewHolder<MediaBrowserCompat.MediaItem>(containerView), LayoutContainer {

  override var model: MediaBrowserCompat.MediaItem? by Delegates.observable(null) { _, _: MediaBrowserCompat.MediaItem?, new: MediaBrowserCompat.MediaItem? ->
    new?.let { bind(it) }
  }

  private val marqueeDelay by lazy { containerView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong() }

  private var lastTheme: Theme? = null

  open fun bind(item: MediaBrowserCompat.MediaItem) {
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

    when {
      item.description.iconBitmap != null -> {
        // Make icon visible
        imgCoverArt.visibility = View.VISIBLE
        tvTrackNumber.visibility = View.GONE

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
      }
      showTrackNumber -> {
        // Set track number
        val trackNumber = item.description.trackNumber % 1000
        tvTrackNumber?.visibility = if (showTrackNumber and (trackNumber > 0)) View.VISIBLE else View.GONE
        imgCoverArt?.visibility = if (showTrackNumber) View.GONE else View.VISIBLE
        tvTrackNumber?.text = trackNumber.toString()
      }
      else -> // Load album art
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

  companion object {
    const val VIEW_TYPE = 8664
  }
}