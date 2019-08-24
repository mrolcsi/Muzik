package hu.mrolcsi.muzik.library.artists

import android.content.res.ColorStateList
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.extensions.getRippleDrawable
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.service.extensions.media.numberOfAlbums
import hu.mrolcsi.muzik.service.extensions.media.numberOfTracks
import hu.mrolcsi.muzik.theme.Theme
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_artist.*
import kotlin.properties.Delegates

class ArtistHolder(override val containerView: View) :
  MVVMViewHolder<MediaBrowserCompat.MediaItem>(containerView), LayoutContainer {

  override var model: MediaBrowserCompat.MediaItem? by Delegates.observable(null) { _, _: MediaBrowserCompat.MediaItem?, new: MediaBrowserCompat.MediaItem? ->
    new?.let { bind(it) }
  }

  private val marqueeDelay = containerView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong()

  private fun bind(item: MediaBrowserCompat.MediaItem) {
    // Set texts
    tvArtist?.run {
      startMarquee(marqueeDelay)
      text = item.description.title
    }

    val numberOfAlbums = item.description.numberOfAlbums
    val numberOfSongs = item.description.numberOfTracks
    val numberOfAlbumsString =
      itemView.context.resources.getQuantityString(R.plurals.artists_numberOfAlbums, numberOfAlbums, numberOfAlbums)
    val numberOfSongsString =
      itemView.context.resources.getQuantityString(R.plurals.artists_numberOfSongs, numberOfSongs, numberOfSongs)
    tvNumberOfSongs?.text =
      itemView.context.getString(R.string.artists_item_subtitle, numberOfAlbumsString, numberOfSongsString)
  }

  fun applyTheme(theme: Theme) {
    itemView.background =
      getRippleDrawable(theme.secondaryForegroundColor, theme.secondaryBackgroundColor)

    tvArtist?.setTextColor(theme.secondaryForegroundColor)
    tvNumberOfSongs?.setTextColor(theme.secondaryForegroundColor)
    tvNumberOfSongs?.setTextColor(theme.secondaryForegroundColor)
    imgChevronRight?.imageTintList = ColorStateList.valueOf(theme.secondaryForegroundColor)
  }
}