package hu.mrolcsi.android.lyricsplayer.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.GlideApp
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.common.DiffCallbackRepository
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_queue.*

class QueueAdapter : ListAdapter<MediaSessionCompat.QueueItem, QueueAdapter.QueueItemHolder>(
  DiffCallbackRepository.queueItemCallback
) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueItemHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_queue, parent, false)
    return QueueItemHolder(itemView)
  }

  override fun onBindViewHolder(holder: QueueItemHolder, position: Int) {
    Log.v(LOG_TAG, "onBindViewHolder($holder, $position")

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

    fun bind(item: MediaSessionCompat.QueueItem) {

      val cached = mCache[item.description.mediaId]

      if (cached != null) {
        imgCoverArt.setImageBitmap(cached.first)
        applyTheme(cached.second)
      } else {
        loadAsync(item)
      }

      tvTitle.text = item.description.title
      tvArtist.text = item.description.subtitle ?: "Unknown Artist"   // TODO: i18n
      tvAlbum.text = item.description.description ?: "Unknown Album"      // TODO: i18n
    }

    private fun loadAsync(item: MediaSessionCompat.QueueItem) {
      imgCoverArt.setImageBitmap(null)
      usedTheme = null

      AsyncTask.execute {
        val metadata = MediaMetadataCompat.Builder().from(item.description).build()

        imgCoverArt.post {
          GlideApp.with(imgCoverArt)
            .load(metadata.albumArt)
            .placeholder(null)
            .into(imgCoverArt)
        }

        // Generate theme
        val albumArt = metadata.albumArt
          ?: BitmapFactory.decodeResource(itemView.resources, R.drawable.placeholder_cover_art)
        val newTheme = ThemeManager.getInstance(itemView.context).createFromBitmap(albumArt)
        itemView.post { applyTheme(newTheme) }
      }
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

      private val mCache = LruCache<String, Pair<Bitmap, Theme>>(20)
    }

  }

  companion object {
    private const val LOG_TAG = "QueueAdapter"
  }
}