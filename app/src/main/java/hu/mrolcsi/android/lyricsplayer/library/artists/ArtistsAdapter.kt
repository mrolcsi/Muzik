package hu.mrolcsi.android.lyricsplayer.library.artists

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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.numberOfAlbums
import hu.mrolcsi.android.lyricsplayer.extensions.numberOfTracks
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager

class ArtistsAdapter : ListAdapter<MediaBrowserCompat.MediaItem, ArtistsAdapter.ArtistHolder>(
  object : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
    override fun areItemsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      return oldItem == newItem
    }

    override fun areContentsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      return oldItem.description.mediaId == newItem.description.mediaId
    }
  }
) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_artist, parent, false)
    return ArtistHolder(view)
  }

  override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
    val item = getItem(position)

    with(holder) {
      // Apply theme
      ThemeManager.currentTheme.value?.let { theme ->
        itemView.background = Theme.getRippleDrawable(theme.darkForegroundColor, theme.darkerBackgroundColor)

        tvArtist?.setTextColor(theme.darkerForegroundColor)
        tvNumOfSongs?.setTextColor(
          ColorUtils.setAlphaComponent(
            theme.darkerForegroundColor,
            Theme.INACTIVE_OPACITY
          )
        )
        imgChevronRight?.imageTintList = ColorStateList.valueOf(theme.darkerForegroundColor)
      }

      // Set texts
      tvArtist?.text = item.description.title
      val numberOfAlbums = item.description.extras?.numberOfAlbums ?: 0
      val numberOfSongs = item.description.extras?.numberOfTracks ?: 0
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