package hu.mrolcsi.muzik.library.artists

import android.content.Context
import android.content.res.ColorStateList
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SectionIndexer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsFragmentArgs
import hu.mrolcsi.muzik.service.extensions.media.numberOfAlbums
import hu.mrolcsi.muzik.service.extensions.media.numberOfTracks
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_artist.*

class ArtistsAdapter(context: Context) :
  MediaItemListAdapter<ArtistsAdapter.ArtistHolder>(context),
  SectionIndexer {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_artist, parent, false)
    return ArtistHolder(view)
  }

  override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
    val item = getItem(position)

    with(holder) {
      // Apply theme
      ThemeManager.getInstance(holder.itemView.context).currentTheme.value?.let { theme ->
        holder.applyTheme(theme)
      }

      bind(item)

      // Set onClickListener
      itemView.setOnClickListener {
        it.findNavController().navigate(
          R.id.action_artists_to_artistDetails,
          ArtistDetailsFragmentArgs(item).toBundle()
        )
      }
    }
  }

  class ArtistHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private val marqueeDelay = containerView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong()

    fun bind(item: MediaBrowserCompat.MediaItem) {
      // Set texts
      tvArtist?.run {
        startMarquee(marqueeDelay)
        text = item.description.title
      }

      val numberOfAlbums = item.description.numberOfAlbums
      val numberOfSongs = item.description.numberOfTracks
      val numberOfAlbumsString =
        itemView.context.resources.getQuantityString(R.plurals.artists_numberOfAlbums, numberOfAlbums, numberOfAlbums)
      val numberOfSongsString =
        itemView.context.resources.getQuantityString(R.plurals.artists_numberOfSongs, numberOfSongs, numberOfSongs)
      tvNumberOfSongs?.text =
        itemView.context.getString(R.string.artists_item_subtitle, numberOfAlbumsString, numberOfSongsString)
    }

    fun applyTheme(theme: Theme) {
      itemView.background = Theme.getRippleDrawable(theme.tertiaryForegroundColor, theme.tertiaryBackgroundColor)

      tvArtist?.setTextColor(theme.tertiaryForegroundColor)
      tvNumberOfSongs?.setTextColor(theme.tertiaryForegroundColor)
      tvNumberOfSongs?.setTextColor(theme.tertiaryForegroundColor)
      imgChevronRight?.imageTintList = ColorStateList.valueOf(theme.tertiaryForegroundColor)
    }
  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "ArtistsAdapter"
  }
}