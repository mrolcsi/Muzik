package hu.mrolcsi.muzik.player.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.DiffCallbackRepository
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.extensions.millisecondsToTimeStamp
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_playlist.*
import kotlin.properties.Delegates

class PlaylistAdapter(
  private val onItemClickListener: OnItemClickListener<PlayQueueEntry, PlaylistViewHolder>
) : ListAdapter<PlayQueueEntry, PlaylistAdapter.PlaylistViewHolder>(
  DiffCallbackRepository.playQueueEntryCallback
) {

  var activeQueueId by Delegates.observable(-1L) { _, old, new ->
    // When active item changes, update rows
    if (old != new) {
      notifyItemChanged(old.toInt())
      notifyItemChanged(new.toInt())
    }
  }

  var isPlaying by Delegates.observable(false) { _, old, new ->
    if (old != new) {
      notifyItemChanged(activeQueueId.toInt())
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_playlist, parent, false)
    return PlaylistViewHolder(itemView)
  }

  override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {

    // Apply theme
    ThemeManager.getInstance(holder.itemView.context).currentTheme.value?.let { theme ->
      holder.applyTheme(theme)
    }

    val item = getItem(position)

    // Hide/Show Now Playing indicator
    if (item._id == activeQueueId) {
      if (isPlaying) {
        holder.imgNowPlaying.setImageResource(R.drawable.ic_media_play)
      } else {
        holder.imgNowPlaying.setImageResource(R.drawable.ic_media_pause)
      }
    }
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

    private val marqueeDelay = containerView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong()

    fun bind(item: PlayQueueEntry) {
      tvTitle?.run {
        text = item.title
        startMarquee(marqueeDelay)
      }

      tvArtist?.run {
        text = item.artist
        startMarquee(marqueeDelay)
      }

      tvTrackNumber?.text = (item._id + 1).toString()

      tvDuration?.text = item.duration?.millisecondsToTimeStamp()
    }

    fun applyTheme(theme: Theme) {
      // Apply colors
      tvTrackNumber?.setTextColor(theme.primaryForegroundColor)
      tvTitle?.setTextColor(theme.primaryForegroundColor)
      tvArtist?.setTextColor(theme.primaryForegroundColor)
      tvDuration?.setTextColor(theme.primaryForegroundColor)
      imgNowPlaying?.setColorFilter(theme.primaryForegroundColor)

      itemView.background = Theme.getRippleDrawable(theme.primaryForegroundColor, theme.primaryBackgroundColor)
    }
  }
}
