package hu.mrolcsi.muzik.player.playlist

import android.net.Uri
import android.view.View
import androidx.core.view.isVisible
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.extensions.millisecondsToTimeStamp
import kotlinx.android.synthetic.main.list_item_playlist.view.*
import kotlin.properties.Delegates

class PlaylistItemHolder(itemView: View) : MVVMViewHolder<PlaylistItem>(itemView) {

  override var model: PlaylistItem? by Delegates.observable(null) { _, old: PlaylistItem?, new: PlaylistItem? ->
    new?.let { bind(it) }
  }

  private val coverArtUri = "content://media/external/audio/media/%d/albumart"

  private fun bind(item: PlaylistItem) {
    itemView.tvSongTitle.text = item.entry.title
    itemView.tvSongArtist.text = item.entry.artist
    itemView.tvDuration?.text = item.entry.duration?.millisecondsToTimeStamp()

    GlideApp.with(itemView.imgCoverArt)
      .load(Uri.parse(String.format(coverArtUri, item.entry.mediaId)))
      .into(itemView.imgCoverArt)

    itemView.imgNowPlaying.isVisible = item.isPlaying
  }
}