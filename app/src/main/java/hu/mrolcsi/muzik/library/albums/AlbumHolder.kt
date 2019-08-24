package hu.mrolcsi.muzik.library.albums

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.extensions.getRippleDrawable
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.theme.Theme
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_album_content.*
import kotlin.properties.Delegates

class AlbumHolder(override val containerView: View) :
  MVVMViewHolder<MediaBrowserCompat.MediaItem>(containerView), LayoutContainer {

  override var model: MediaBrowserCompat.MediaItem? by Delegates.observable(null) { _, _: MediaBrowserCompat.MediaItem?, new: MediaBrowserCompat.MediaItem? ->
    new?.let { bind(it) }
  }

  private val marqueeDelay = containerView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong()

  private fun bind(item: MediaBrowserCompat.MediaItem) {

    // Set texts
    tvAlbumTitle?.run {
      text = item.description.title
      startMarquee(marqueeDelay)
    }
    tvAlbumArtist?.run {
      text = item.description.subtitle
      startMarquee(marqueeDelay)
    }

    ViewCompat.setTransitionName(imgCoverArt, "coverArt${item.description.id}")

    // Load album art
    GlideApp.with(imgCoverArt)
      .asBitmap()
      .load(item.description.albumArtUri)
      .onResourceReady { /* TODO: create theme */ }
      .into(imgCoverArt)
  }

  private fun applyTheme(theme: Theme) {
    (itemView as CardView).setCardBackgroundColor(theme.primaryBackgroundColor)
    itemView.foreground =
      getRippleDrawable(theme.primaryBackgroundColor, theme.primaryForegroundColor)

    tvAlbumTitle?.setTextColor(theme.primaryForegroundColor)
    tvAlbumArtist?.setTextColor(theme.primaryForegroundColor)
  }
}