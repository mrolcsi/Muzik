package hu.mrolcsi.muzik.library.albums

import android.graphics.Bitmap
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.BuildConfig
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.DiffCallbackRepository
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.library.albums.details.AlbumDetailsFragmentArgs
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_album.*

class AlbumsAdapter : ListAdapter<MediaBrowserCompat.MediaItem, AlbumsAdapter.AlbumHolder>(
  DiffCallbackRepository.mediaItemCallback
) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_album, parent, false)
    return AlbumHolder(itemView)
  }

  override fun onBindViewHolder(holder: AlbumHolder, position: Int) = holder.bind(getItem(position))

  class AlbumHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private val onCoverArtReady = object : RequestListener<Bitmap> {
      override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Bitmap>?,
        isFirstResource: Boolean
      ): Boolean {
        return false
      }

      override fun onResourceReady(
        resource: Bitmap?,
        model: Any?,
        target: Target<Bitmap>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
      ): Boolean {
        resource?.let { bitmap ->
          AsyncTask.execute {
            // Generate theme from resource
            val theme = ThemeManager.getInstance(containerView.context).createFromBitmap(bitmap)
            containerView.post {
              applyTheme(theme)
            }
          }
        }

        return false
      }
    }

    fun bind(item: MediaBrowserCompat.MediaItem) {

      // Set texts
      tvAlbumTitle?.text = item.description.title
      tvAlbumArtist?.text = item.description.subtitle

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
          with(it.findNavController()) {

            val extras = FragmentNavigatorExtras(
              imgCoverArt to "coverArt"
            )

            val args = AlbumDetailsFragmentArgs(item).toBundle()

            navigate(R.id.navigation_albumDetails, args, null, extras)

            //navigate(AlbumsFragmentDirections.actionAlbumsToAlbumDetails(item), extras)
          }
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