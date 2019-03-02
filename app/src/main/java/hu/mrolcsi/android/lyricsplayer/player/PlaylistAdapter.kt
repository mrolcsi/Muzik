package hu.mrolcsi.android.lyricsplayer.player

import android.support.v4.media.session.MediaSessionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration
import hu.mrolcsi.android.lyricsplayer.extensions.millisecondsToTimeStamp
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_playlist.*

class PlaylistAdapter : ListAdapter<MediaSessionCompat.QueueItem, PlaylistAdapter.PlaylistViewHolder>(
  object : DiffUtil.ItemCallback<MediaSessionCompat.QueueItem>() {
    override fun areItemsTheSame(
      oldItem: MediaSessionCompat.QueueItem,
      newItem: MediaSessionCompat.QueueItem
    ): Boolean {
      return oldItem.queueId == newItem.queueId
    }

    override fun areContentsTheSame(
      oldItem: MediaSessionCompat.QueueItem,
      newItem: MediaSessionCompat.QueueItem
    ): Boolean {
      return oldItem == newItem
    }
  }
) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_playlist, parent, false)
    return PlaylistViewHolder(itemView)
  }

  override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {

// Apply theme
    ThemeManager.currentTheme.value?.let { theme ->
      holder.tvTitle.setTextColor(theme.foregroundColor)
      holder.tvArtist.setTextColor(theme.foregroundColor)
      holder.tvDuration.setTextColor(theme.foregroundColor)
    }

    holder.bind(getItem(position))
  }

  class PlaylistViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: MediaSessionCompat.QueueItem) {
      with(item.description) {
        Glide.with(imgCoverArt)
          .load(this.albumArt)
          .into(imgCoverArt)

        tvTitle.text = this.title
        tvArtist.text = this.subtitle
        tvDuration.text = this.duration.millisecondsToTimeStamp()
      }
    }
  }
}
