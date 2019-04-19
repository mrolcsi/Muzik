package hu.mrolcsi.muzik.library.artists

import android.content.res.ColorStateList
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.DiffCallbackRepository
import hu.mrolcsi.muzik.service.extensions.media.numberOfAlbums
import hu.mrolcsi.muzik.service.extensions.media.numberOfTracks
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager

class ArtistsAdapter : ListAdapter<MediaBrowserCompat.MediaItem, ArtistsAdapter.ArtistHolder>(
  DiffCallbackRepository.mediaItemCallback
) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_artist, parent, false)
    return ArtistHolder(view)
  }

  override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
    val item = getItem(position)

    with(holder) {
      // Apply theme
      ThemeManager.getInstance(holder.itemView.context).currentTheme.value?.let { theme ->
        itemView.background = Theme.getRippleDrawable(theme.secondaryForegroundColor, theme.tertiaryBackgroundColor)

        tvArtist?.setTextColor(theme.tertiaryForegroundColor)
        tvNumOfSongs?.setTextColor(
          ColorUtils.setAlphaComponent(
            theme.tertiaryForegroundColor,
            Theme.SUBTITLE_OPACITY
          )
        )
        imgChevronRight?.imageTintList = ColorStateList.valueOf(theme.tertiaryForegroundColor)
      }

      // Set texts
      tvArtist?.text = item.description.title
      val numberOfAlbums = item.description.numberOfAlbums
      val numberOfSongs = item.description.numberOfTracks
      val numberOfAlbumsString =
        itemView.context.resources.getQuantityString(R.plurals.artists_numberOfAlbums, numberOfAlbums, numberOfAlbums)
      val numberOfSongsString =
        itemView.context.resources.getQuantityString(R.plurals.artists_numberOfSongs, numberOfSongs, numberOfSongs)
      tvNumOfSongs?.text =
        itemView.context.getString(R.string.artists_item_subtitle, numberOfAlbumsString, numberOfSongsString)

      // Set onClickListener
      itemView.setOnClickListener {
        with(it.findNavController()) {
          try {
            val direction = ArtistsFragmentDirections.actionArtistsToAlbums(
              item.mediaId,
              item.description.title.toString(),
              numberOfSongs
            )
            navigate(direction)
          } catch (e: IllegalArgumentException) {
            Toast.makeText(it.context, "Lost navigation.", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
  }

  class ArtistHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvArtist: TextView? = itemView.findViewById(R.id.tvTitle)
    val tvNumOfSongs: TextView? = itemView.findViewById(R.id.tvSubtitle)
    val imgChevronRight: ImageView? = itemView.findViewById(R.id.imgChevronRight)
  }

  companion object {
    private const val LOG_TAG = "ArtistsAdapter"
  }
}