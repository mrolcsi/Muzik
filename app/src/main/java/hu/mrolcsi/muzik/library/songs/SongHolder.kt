package hu.mrolcsi.muzik.library.songs

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import androidx.core.view.isVisible
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.extensions.millisecondsToTimeStamp
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import hu.mrolcsi.muzik.service.extensions.media.duration
import hu.mrolcsi.muzik.service.extensions.media.isNowPlaying
import hu.mrolcsi.muzik.service.extensions.media.trackNumber
import kotlinx.android.synthetic.main.list_item_song.view.*
import kotlin.properties.Delegates

open class SongHolder(itemView: View, private val showTrackNumber: Boolean) :
  MVVMViewHolder<MediaBrowserCompat.MediaItem>(itemView) {

  override var model: MediaBrowserCompat.MediaItem? by Delegates.observable(null) { _, _: MediaBrowserCompat.MediaItem?, new: MediaBrowserCompat.MediaItem? ->
    new?.let { bind(it) }
  }

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

      if (showTrackNumber) {
        // Set track number
        val trackNumber = item.description.trackNumber % 1000
        tvTrackNumber?.isVisible = showTrackNumber && (trackNumber > 0)
        coverArtContainer?.isVisible = !showTrackNumber
        tvTrackNumber?.text = trackNumber.toString()
      } else {
        // Load album art
        GlideApp.with(imgCoverArt)
          .load(item.description.coverArtUri)
          .into(imgCoverArt)
      }

      imgNowPlaying.isVisible = item.description.isNowPlaying
    }
  }

  companion object {
    const val VIEW_TYPE = 8664
  }
}