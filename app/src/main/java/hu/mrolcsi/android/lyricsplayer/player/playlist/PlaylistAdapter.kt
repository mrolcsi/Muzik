package hu.mrolcsi.android.lyricsplayer.player.playlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.common.DiffCallbackRepository
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.PlayQueueEntry
import hu.mrolcsi.android.lyricsplayer.extensions.OnItemClickListener
import hu.mrolcsi.android.lyricsplayer.extensions.millisecondsToTimeStamp
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_playlist.*

class PlaylistAdapter(
  private val onItemClickListener: OnItemClickListener<PlayQueueEntry, PlaylistViewHolder>
) : ListAdapter<PlayQueueEntry, PlaylistAdapter.PlaylistViewHolder>(
  DiffCallbackRepository.playQueueEntryCallback
) {

  var activeQueueId: Long = -1

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_playlist, parent, false)
    return PlaylistViewHolder(itemView)
  }

  override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {

    // Apply theme
    ThemeManager.getInstance(holder.itemView.context).currentTheme.value?.let { theme ->
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
    holder.imgNowPlaying.visibility = if (item._id == activeQueueId) View.VISIBLE else View.GONE
    holder.tvTrackNumber.visibility = if (item._id != activeQueueId) View.VISIBLE else View.GONE

    holder.itemView.setOnClickListener {
      // Show Now Playing indicator
      onItemClickListener.onItemClick(item, holder, position, getItemId(position))
    }
    holder.bind(item)
  }

  override fun getItemId(position: Int): Long {
    return getItem(position)._id
  }

  class PlaylistViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: PlayQueueEntry) {
      tvTrackNumber.text = (item._id + 1).toString()
      tvTitle.text = item.title
      tvArtist.text = item.artist
      tvDuration.text = item.duration?.millisecondsToTimeStamp()
    }
  }
}
