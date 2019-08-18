package hu.mrolcsi.muzik.player.playlist

import android.content.res.ColorStateList
import android.net.Uri
import android.view.ViewGroup
import androidx.core.view.isVisible
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.extensions.millisecondsToTimeStamp
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.list_item_playlist.view.*
import kotlin.properties.Delegates

class PlaylistItemHolder(parent: ViewGroup) : MVVMViewHolder<PlaylistItem>(R.layout.list_item_playlist, parent) {

  override var model: PlaylistItem? by Delegates.observable(null) { _, old: PlaylistItem?, new: PlaylistItem? ->
    new?.let { bind(it) }
  }

  private val coverArtUri = "content://media/external/audio/media/%d/albumart"

  private val marqueeDelay = itemView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong()

  fun bind(item: PlaylistItem) {
    // Apply theme
    ThemeManager.getInstance(itemView.context).currentTheme.value?.let { theme ->
      applyTheme(theme)
    }

    itemView.tvSongTitle?.run {
      text = item.entry.title
      startMarquee(marqueeDelay)
    }

    itemView.tvSongArtist?.run {
      text = item.entry.artist
      startMarquee(marqueeDelay)
    }

    itemView.tvDuration?.text = item.entry.duration?.millisecondsToTimeStamp()

    GlideApp.with(itemView.imgCoverArt)
      .load(Uri.parse(String.format(coverArtUri, item.entry.mediaId)))
      .into(itemView.imgCoverArt)

    itemView.imgNowPlaying.isVisible = item.isPlaying
  }

  private fun applyTheme(theme: Theme) {
    // Apply colors
    itemView.tvSongTitle?.setTextColor(theme.primaryForegroundColor)
    itemView.tvSongArtist?.setTextColor(theme.primaryForegroundColor)
    itemView.tvDuration?.setTextColor(theme.primaryForegroundColor)
    itemView.imgNowPlaying?.imageTintList = ColorStateList.valueOf(theme.primaryForegroundColor)

    itemView.backgroundTintList = ColorStateList.valueOf(theme.primaryBackgroundColor)
  }
}