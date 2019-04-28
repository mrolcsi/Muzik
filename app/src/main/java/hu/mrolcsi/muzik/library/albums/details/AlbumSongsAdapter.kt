package hu.mrolcsi.muzik.library.albums.details

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.library.songs.SongsAdapter
import hu.mrolcsi.muzik.service.theme.Theme
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

  override fun onBindViewHolder(holder: SongHolder, position: Int) {
    val item = getItem(position)

    // Apply theme
    theme?.let {
      holder.applyTheme(it)
    }

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