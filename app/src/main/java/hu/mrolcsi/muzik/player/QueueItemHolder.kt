package hu.mrolcsi.muzik.player

import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.view.ViewGroup
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onLoadFailed
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import hu.mrolcsi.muzik.theme.Theme
import kotlinx.android.synthetic.main.list_item_queue.view.*
import kotlin.properties.Delegates

class QueueItemHolder(parent: ViewGroup) : MVVMViewHolder<QueueItem>(R.layout.list_item_queue, parent) {

  override var model: QueueItem? by Delegates.observable(null) { _, old: QueueItem?, new: QueueItem? ->
    new?.let { bind(it) }
  }

  private val marqueeDelay = itemView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong()

  var usedTheme: Theme? = null

  private fun bind(item: QueueItem) {
    itemView.run {
      GlideApp.with(imgCoverArt)
        .asBitmap()
        .load(item.description.coverArtUri)
        .override(Target.SIZE_ORIGINAL)
        .onResourceReady { albumArt -> /* TODO: create theme */ }
        .onLoadFailed { usedTheme = null; false }
        .into(imgCoverArt)

      tvTitle?.run {
        text = item.description.title
        startMarquee(marqueeDelay)
      }

      tvArtist?.run {
        text = item.description.subtitle ?: "Unknown Artist"   // TODO: i18n
        startMarquee(marqueeDelay)
      }

      tvAlbum?.run {
        text = item.description.description ?: "Unknown Album"      // TODO: i18n
        startMarquee(marqueeDelay)
      }
    }
  }

  private fun applyTheme(theme: Theme) {
    itemView.run {
      tvTitle.setTextColor(theme.primaryForegroundColor)
      tvArtist.setTextColor(theme.primaryForegroundColor)
      tvAlbum.setTextColor(theme.primaryForegroundColor)
    }

    usedTheme = theme

    itemId
  }


  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "QueueItemHolder"
  }

}