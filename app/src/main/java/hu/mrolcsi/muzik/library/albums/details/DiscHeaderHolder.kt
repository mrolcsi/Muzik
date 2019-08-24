package hu.mrolcsi.muzik.library.albums.details

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import hu.mrolcsi.muzik.library.songs.SongHolder
import hu.mrolcsi.muzik.theme.Theme
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_disc_number.*

class DiscHeaderHolder(containerView: View) :
  SongHolder(containerView, false), LayoutContainer {

  override fun bind(item: MediaBrowserCompat.MediaItem) {
    tvDiscNumber.text = item.description.title
  }

  override fun applyTheme(theme: Theme) {
    imgDisc.drawable.setTint(theme.secondaryForegroundColor)
    tvDiscNumber.setTextColor(theme.secondaryForegroundColor)
    divider.background.setTint(theme.secondaryForegroundColor)
  }

  companion object {
    const val VIEW_TYPE = 3482
  }
}