package hu.mrolcsi.muzik.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
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
import hu.mrolcsi.muzik.service.extensions.media.albumArt
import hu.mrolcsi.muzik.service.extensions.media.from
import hu.mrolcsi.muzik.theme.Theme
import hu.mrolcsi.muzik.theme.ThemeManager
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
          val metadata = MediaMetadataCompat.Builder().from(item.description).build()
          val albumArt = metadata.albumArt
            ?: BitmapFactory.decodeResource(holder.itemView.resources, R.drawable.placeholder_cover_art)
          val theme = ThemeManager.getInstance(holder.itemView.context).createFromBitmap(albumArt)

          mCache.put(key, albumArt to theme)
        }
      }
    }

    holder.bind(getItem(position))
  }

//  override fun getItem(position: Int): MediaSessionCompat.QueueItem {
//    return if (realItemCount == 0) {
//      super.getItem(position)
//    } else {
//      super.getItem(position % realItemCount)
//    }
//  }

//  override fun getItemCount(): Int {
//    // To enable infinite scrolling
//    return if (super.getItemCount() == 0) 0 else Int.MAX_VALUE
//  }

  //val realItemCount get() = super.getItemCount()

  override fun getItemId(position: Int): Long {
    return getItem(position).queueId
  }

  fun getItemPositionById(id: Long /*, startingPosition: Int 0= 0*/): Int {
//    val start = Math.max(0, startingPosition - realItemCount / 2)
//    val end = Math.min(Int.MAX_VALUE, startingPosition + realItemCount / 2)
    for (i in 0 until itemCount) {
      if (getItemId(i) == id) {
        return i
      }
    }
    return RecyclerView.NO_POSITION
  }

  class QueueItemHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    var usedTheme: Theme? = null

    fun bind(item: MediaSessionCompat.QueueItem) {
      val key = item.description.mediaId

      val cached = mCache[key]
      if (cached != null) {
        Log.v(LOG_TAG, "Cache hit : $key")

        // Load item into cache
        val metadata = MediaMetadataCompat.Builder().from(item.description).build()
        val albumArt = metadata.albumArt
          ?: BitmapFactory.decodeResource(itemView.resources, R.drawable.placeholder_cover_art)
        val theme = ThemeManager.getInstance(itemView.context).createFromBitmap(albumArt)
        mCache.put(key, albumArt to theme)

        GlideApp.with(imgCoverArt)
          .load(cached.first)
          .into(imgCoverArt)
        //imgCoverArt.setImageBitmap(cached.first)
        applyTheme(cached.second)
      } else {
        Log.w(LOG_TAG, "Cache miss: $key")

        val metadata = MediaMetadataCompat.Builder().from(item.description).build()
        val albumArt = metadata.albumArt
          ?: BitmapFactory.decodeResource(itemView.context.resources, R.drawable.placeholder_cover_art)
        val theme = ThemeManager.getInstance(itemView.context).createFromBitmap(albumArt)

        GlideApp.with(imgCoverArt)
          .load(albumArt)
          .into(imgCoverArt)
        //imgCoverArt.setImageBitmap(albumArt)
        applyTheme(theme)
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