package hu.mrolcsi.muzik.ui.albumDetails

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import hu.mrolcsi.muzik.ui.songs.SongHolder
import kotlinx.android.synthetic.main.list_item_disc_number.view.*

class DiscHeaderHolder(itemView: View) : SongHolder(itemView, false) {

  override fun bind(item: MediaBrowserCompat.MediaItem) {
    itemView.tvDiscNumber.text = item.description.title
  }

  companion object {
    const val VIEW_TYPE = 3482
  }
}