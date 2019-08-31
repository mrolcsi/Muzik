package hu.mrolcsi.muzik.library.songs

import android.graphics.drawable.InsetDrawable
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onResourceReadyWithTarget
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.extensions.millisecondsToTimeStamp
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import hu.mrolcsi.muzik.service.extensions.media.duration
import hu.mrolcsi.muzik.service.extensions.media.trackNumber
import kotlinx.android.synthetic.main.list_item_song.view.*
import kotlin.properties.Delegates

open class SongHolder(itemView: View, private val showTrackNumber: Boolean) :
  MVVMViewHolder<MediaBrowserCompat.MediaItem>(itemView) {

  override var model: MediaBrowserCompat.MediaItem? by Delegates.observable(null) { _, _: MediaBrowserCompat.MediaItem?, new: MediaBrowserCompat.MediaItem? ->
    new?.let { bind(it) }
  }

  private val marqueeDelay by lazy { itemView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong() }

  open fun bind(item: MediaBrowserCompat.MediaItem) {
    itemView.run {
      // Set texts
      tvSongTitle.text = item.description.title
      tvSongArtist.run {
        if (item.description.subtitle != null) {
          visibility = View.VISIBLE
          text = item.description.subtitle
        } else {
          visibility = View.GONE
        }
      }
      tvDuration.text = item.description.duration.takeIf { it > 0 }?.millisecondsToTimeStamp()

      when {
        item.description.iconBitmap != null -> {
          // Make icon visible
          imgCoverArt.visibility = View.VISIBLE
          tvTrackNumber.visibility = View.GONE

          // Load icon
          GlideApp.with(imgCoverArt)
            .asDrawable()
            .load(item.description.iconBitmap)
            .onResourceReadyWithTarget { target, resource ->
              // TODO: Replace Shuffle All with FAB
              target.getSize { width, height ->
                // Add artificial padding using an InsetDrawable
                val drawable = InsetDrawable(resource, width / 3, height / 3, width / 3, height / 3)
                target.onResourceReady(drawable, null)
              }
              true
            }
            .into(imgCoverArt)
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
  }

  companion object {
    const val VIEW_TYPE = 8664
  }
}