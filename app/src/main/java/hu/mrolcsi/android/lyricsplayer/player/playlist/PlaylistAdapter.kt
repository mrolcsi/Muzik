package hu.mrolcsi.android.lyricsplayer.player.playlist

import android.graphics.Color
import android.support.v4.media.session.MediaSessionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.OnItemClickListener
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration
import hu.mrolcsi.android.lyricsplayer.extensions.millisecondsToTimeStamp
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_playlist.*

class PlaylistAdapter(
  private val onItemClickListener: OnItemClickListener<MediaSessionCompat.QueueItem, PlaylistViewHolder>
) : ListAdapter<MediaSessionCompat.QueueItem, PlaylistAdapter.PlaylistViewHolder>(
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

  var activeQueueId: Long = -1

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_playlist, parent, false)
    return PlaylistViewHolder(itemView)
  }

  override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {

    // Apply theme
    ThemeManager.currentTheme.value?.let { theme ->
      // Apply colors
      holder.tvTrackNumber.setTextColor(theme.primaryForegroundColor)
      holder.tvTitle.setTextColor(theme.primaryForegroundColor)
      holder.tvArtist.setTextColor(theme.primaryForegroundColor)
      holder.tvDuration.setTextColor(theme.primaryForegroundColor)
      holder.imgNowPlaying.setColorFilter(theme.primaryForegroundColor)

      holder.itemView.background = Theme.getRippleDrawable(
        ColorUtils.setAlphaComponent(theme.primaryForegroundColor, Theme.DISABLED_OPACITY),
        Color.TRANSPARENT
      )
    }

    val item = getItem(position)

    // Hide Now Playing indicator
    val activeQueueId = activeQueueId
    holder.imgNowPlaying.visibility = if (item.queueId == activeQueueId) View.VISIBLE else View.GONE
    holder.tvTrackNumber.visibility = if (item.queueId != activeQueueId) View.VISIBLE else View.GONE

    holder.itemView.setOnClickListener {
      // Show Now Playing indicator
      onItemClickListener.onItemClick(item, holder, position, getItemId(position))
    }
    holder.bind(item)
  }

  override fun getItemId(position: Int): Long {
    return getItem(position).queueId
  }

  class PlaylistViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: MediaSessionCompat.QueueItem) {
      with(item.description) {
        tvTrackNumber.text = item.queueId.toString()
        tvTitle.text = this.title
        tvArtist.text = this.subtitle
        tvDuration.text = this.duration.millisecondsToTimeStamp()
      }
    }
  }
}
