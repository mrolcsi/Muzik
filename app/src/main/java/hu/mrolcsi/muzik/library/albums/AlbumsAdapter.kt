package hu.mrolcsi.muzik.library.albums

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SectionIndexer
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.BuildConfig
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.MuzikGlideModule
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.library.albums.details.AlbumDetailsFragmentArgs
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_album_content.*

class AlbumsAdapter(context: Context, @RecyclerView.Orientation private val orientation: Int) :
  MediaItemListAdapter<AlbumsAdapter.AlbumHolder>(context),
  SectionIndexer {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
    val itemView = if (orientation == RecyclerView.HORIZONTAL) {
      LayoutInflater.from(parent.context).inflate(R.layout.list_item_album_horizontal, parent, false)
    } else {
      LayoutInflater.from(parent.context).inflate(R.layout.list_item_album_vertical, parent, false)
    }
    return AlbumHolder(itemView)
  }

  override fun onBindViewHolder(holder: AlbumHolder, position: Int) = holder.bind(getItem(position))

  class AlbumHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private val marqueeDelay = containerView.resources.getInteger(R.integer.preferredMarqueeDelay).toLong()

    private val onCoverArtReady = object : MuzikGlideModule.SimpleRequestListener<Bitmap> {
      override fun onLoadFailed() {}

      override fun onResourceReady(resource: Bitmap?) {
        resource?.let { bitmap ->
          AsyncTask.execute {
            // Generate theme from resource
            val theme = ThemeManager.getInstance(containerView.context).createFromBitmap(bitmap)
            containerView.post {
              applyTheme(theme)
            }
          }
        }
      }
    }

    fun bind(item: MediaBrowserCompat.MediaItem) {

      // Set texts
      tvAlbumTitle?.run {
        text = item.description.title
        startMarquee(marqueeDelay)
      }
      tvAlbumArtist?.run {
        text = item.description.subtitle
        startMarquee(marqueeDelay)
      }

      ViewCompat.setTransitionName(imgCoverArt, "coverArt${item.description.id}")

      // Load album art
      GlideApp.with(imgCoverArt)
        .asBitmap()
        .load(item.description.albumArtUri)
        .addListener(onCoverArtReady)
        .into(imgCoverArt)

      // Set onClickListener
      if (item.mediaId == MEDIA_ID_ALL_SONGS) {
        // TODO
      } else {
        itemView.setOnClickListener {
          it.findNavController().navigate(
            R.id.navigation_albumDetails,
            AlbumDetailsFragmentArgs(item).toBundle(),
            null,
            FragmentNavigatorExtras(imgCoverArt to "coverArt")
          )
        }
      }
    }

    private fun applyTheme(theme: Theme) {
      (itemView as CardView).setCardBackgroundColor(theme.primaryBackgroundColor)
      itemView.foreground = Theme.getRippleDrawable(theme.primaryBackgroundColor, theme.primaryForegroundColor)

      tvAlbumTitle?.setTextColor(theme.primaryForegroundColor)
      tvAlbumArtist?.setTextColor(theme.primaryForegroundColor)
    }
  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "AlbumsAdapter"

    const val MEDIA_ID_ALL_SONGS = BuildConfig.APPLICATION_ID + ".ALL_SONGS"
  }
}