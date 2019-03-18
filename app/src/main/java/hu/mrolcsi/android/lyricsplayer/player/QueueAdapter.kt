package hu.mrolcsi.android.lyricsplayer.player

import android.graphics.Bitmap
import android.os.AsyncTask
import android.support.v4.media.MediaMetadataCompat
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.common.DiffCallbackRepository
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.PlayQueueEntry
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_queue.*

class QueueAdapter : ListAdapter<PlayQueueEntry, QueueAdapter.QueueItemHolder>(
  DiffCallbackRepository.playQueueEntryCallback
) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueItemHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_queue, parent, false)
    return QueueItemHolder(itemView)
  }

  override fun onBindViewHolder(holder: QueueItemHolder, position: Int) {
    holder.bind(getItem(position))
  }

  override fun getItemId(position: Int): Long {
    return getItem(position)._id
  }

  class QueueItemHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    var usedTheme: Theme? = null

    fun bind(item: PlayQueueEntry) {

      usedTheme = null

      val cached = mCache[item._data]

      if (cached != null) {
        imgCoverArt.setImageBitmap(cached.first)
        applyTheme(cached.second)
      } else {
        loadAsync(item)
      }

      tvTitle.text = item.title
      tvArtist.text = item.artist ?: "Unknown Artist"   // TODO: i18n
      tvAlbum.text = item.album ?: "Unknown Album"      // TODO: i18n
    }

    private fun loadAsync(item: PlayQueueEntry) {
      AsyncTask.execute {
        val metadata = MediaMetadataCompat.Builder().from(item._data).build()

        itemView.post {
          // set image
          imgCoverArt.setImageBitmap(metadata.albumArt)
        }

        // Generate theme
        metadata.albumArt?.let { albumArt ->
          // Generate theme
          Palette.from(albumArt)
            .clearFilters()
            .generate { mainPalette ->
              mainPalette?.let { ThemeManager.getInstance(itemView.context).createTheme(it) }?.let { theme ->
                // Add StatusBar color to theme
                Palette.from(albumArt)
                  .clearFilters()
                  .setRegion(0, 0, albumArt.width, 48)
                  .generate {
                    it?.let { statusBarPalette ->
                      theme.statusBarColor = statusBarPalette.swatches.maxBy { swatch ->
                        swatch.population
                      }?.rgb ?: theme.primaryBackgroundColor

                      // Store items in cache
                      mCache.put(item._data, Pair(albumArt, theme))

                      applyTheme(theme)
                    }
                  }
              }
            }
        }
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
}