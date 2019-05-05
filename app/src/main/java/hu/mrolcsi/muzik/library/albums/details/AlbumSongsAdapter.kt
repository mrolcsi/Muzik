package hu.mrolcsi.muzik.library.albums.details

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.library.songs.SongsAdapter
import hu.mrolcsi.muzik.service.theme.Theme
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_disc_number.*
import kotlin.properties.Delegates

class AlbumSongsAdapter(
  context: Context,
  onItemClickListener: OnItemClickListener<MediaBrowserCompat.MediaItem, SongHolder>? = null
) : SongsAdapter(context, onItemClickListener) {

  var theme: Theme? by Delegates.observable<Theme?>(null) { _, old, new ->
    if (old != new) {
      notifyDataSetChanged()
    }
  }

  override fun getItemViewType(position: Int): Int =
    if (getItem(position).isBrowsable) VIEW_TYPE_SECTION else VIEW_TYPE_ITEM

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongHolder {

    if (viewType == VIEW_TYPE_SECTION) {
      return SectionHeaderHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_disc_number, parent, false)
      )
    }

    return super.onCreateViewHolder(parent, viewType)
  }

  override fun onBindViewHolder(holder: SongHolder, position: Int) {
    val item = getItem(position)

    // Apply theme
    theme?.let {
      holder.applyTheme(it)
    }

    if (holder is SectionHeaderHolder) {

      holder.bind(item)

    } else {

      // Set onClickListener
      onItemClickListener?.run {
        holder.itemView.setOnClickListener {
          this.onItemClick(item, holder, position, getItemId(position))
        }
      }

      // Bind data
      holder.bind(item, showTrackNumber)
    }
  }

  companion object {
    private const val VIEW_TYPE_ITEM = 0
    private const val VIEW_TYPE_SECTION = 1
  }

  class SectionHeaderHolder(containerView: View) :
    SongHolder(containerView), LayoutContainer {

    fun bind(item: MediaBrowserCompat.MediaItem) {
      tvDiscNumber.text = item.description.title
    }

    override fun applyTheme(theme: Theme) {
      imgDisc.drawable.setTint(theme.tertiaryForegroundColor)
      tvDiscNumber.setTextColor(theme.tertiaryForegroundColor)
      divider.background.setTint(theme.tertiaryForegroundColor)
    }
  }
}