package hu.mrolcsi.muzik.player

import android.graphics.Bitmap
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.DiffCallbackRepository
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.MuzikGlideModule
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_queue.*
import java.util.concurrent.Executors

class QueueAdapter : ListAdapter<MediaSessionCompat.QueueItem, QueueAdapter.QueueItemHolder>(
  DiffCallbackRepository.queueItemCallback
) {

  private val mBackgroundExecutor = Executors.newSingleThreadExecutor()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueItemHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_queue, parent, false)
    return QueueItemHolder(itemView)
  }

  override fun onBindViewHolder(holder: QueueItemHolder, position: Int) {
    Log.v(LOG_TAG, "onBindViewHolder($holder, $position")

    // Cache items surrounding position
    val start = Math.max(position - WINDOW_SIZE, 0)
    val end = Math.min(position + WINDOW_SIZE, itemCount)
    for (i in start until end) {
      val item = getItem(i)
      val key = item.description.mediaId

      if (mCache[key] == null) {
        mBackgroundExecutor.submit {
          Log.v(LOG_TAG, "Caching item: $item")

          // Load item into cache
          MediaStore.Images.Media.getBitmap(
            holder.itemView.context.contentResolver,
            item.description.coverArtUri
          )?.let {
            val theme = ThemeManager.getInstance(holder.itemView.context).createFromBitmap(it)
            mCache.put(key, it to theme)
          }
        }
      }
    }

    holder.bind(getItem(position))
  }

  override fun getItemId(position: Int): Long {
    return getItem(position).queueId
  }

  fun getItemPositionById(id: Long): Int {
    for (i in 0 until itemCount) {
      if (getItemId(i) == id) {
        return i
      }
    }
    return RecyclerView.NO_POSITION
  }

  class QueueItemHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    var usedTheme: Theme? = null

    private val onCoverArtReady = object : MuzikGlideModule.SimpleRequestListener<Bitmap> {
      override fun onLoadFailed() {
        usedTheme = null
      }

      override fun onResourceReady(resource: Bitmap?) {
        resource?.let { bitmap ->
          AsyncTask.execute {
            // Generate theme from resource
            val theme = ThemeManager.getInstance(containerView.context).createFromBitmap(bitmap).also {
              usedTheme = it
            }
            containerView.post {
              applyTheme(theme)
            }
          }
        }
      }
    }

    fun bind(item: MediaSessionCompat.QueueItem) {
      val key = item.description.mediaId

      val cached = mCache[key]
      if (cached != null) {
        GlideApp.with(imgCoverArt)
          .asBitmap()
          .load(cached.first)
          .into(imgCoverArt)

        applyTheme(cached.second)
      } else {
        GlideApp.with(imgCoverArt)
          .asBitmap()
          .load(item.description.coverArtUri)
          .addListener(onCoverArtReady)
          .into(imgCoverArt)
      }

      tvTitle.text = item.description.title
      tvArtist.text = item.description.subtitle ?: "Unknown Artist"   // TODO: i18n
      tvAlbum.text = item.description.description ?: "Unknown Album"      // TODO: i18n
    }

    private fun applyTheme(theme: Theme) {
      tvTitle.setTextColor(theme.primaryForegroundColor)
      tvArtist.setTextColor(theme.primaryForegroundColor)
      tvAlbum.setTextColor(theme.primaryForegroundColor)

      usedTheme = theme
    }

    companion object {
      @Suppress("unused")
      private const val LOG_TAG = "QueueItemHolder"
    }

  }

  companion object {
    private const val LOG_TAG = "QueueAdapter"

    private const val WINDOW_SIZE = 3

    // Keep it "static" so it can be accessed from the ViewHolder as well.
    private val mCache = LruCache<String, Pair<Bitmap, Theme>>(10)
  }
}