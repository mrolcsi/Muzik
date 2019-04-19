package hu.mrolcsi.muzik.service.exoplayer

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import hu.mrolcsi.muzik.service.extensions.media.albumArt
import hu.mrolcsi.muzik.service.extensions.media.from
import hu.mrolcsi.muzik.service.extensions.media.id

class ExoMetadataProvider(
  private val mediaController: MediaControllerCompat,
  metadataExtrasPrefix: String? = null,
  private val placeholderAlbumArt: Bitmap? = null,
  private val onCacheUpdated: (MediaMetadataCompat) -> Unit = {}
) : MediaSessionConnector.MediaMetadataProvider {

  private val mDefaultProvider =
    MediaSessionConnector.DefaultMediaMetadataProvider(mediaController, metadataExtrasPrefix)

  private val mMetadataCache = HashMap<String, MediaMetadataCompat?>()

  private val mWindow = Timeline.Window()

  override fun getMetadata(player: Player): MediaMetadataCompat? {
    // Get description from queue
    val description = getDescription(player) ?: return null

    val mediaId = description.mediaId ?: return null

    // Get metadata from cache
    val cachedMetadata = mMetadataCache[mediaId]

    return if (cachedMetadata != null) {
      Log.v(LOG_TAG, "Item in cache: {${cachedMetadata.description}, albumArt?=${cachedMetadata.albumArt}}")
      cachedMetadata
    } else {
      // Start a load in the background then return default metadata
      val defaultMetadata = mDefaultProvider.getMetadata(player)
      AsyncTask.execute {
        val newMetadata = fetchMetadata(defaultMetadata)
        Log.v(LOG_TAG, "Updating cache with {${newMetadata.description}}")
        mMetadataCache[mediaId] = newMetadata
        // Send invalidate to session connector
        Handler(player.applicationLooper).post {
          // Use the same thread as the player, to avoid warnings.
          onCacheUpdated.invoke(newMetadata)
        }
      }
      Log.v(LOG_TAG, "Using default metadata: {${defaultMetadata.description}}")
      mMetadataCache[mediaId] = defaultMetadata
      defaultMetadata
    }
  }

  /**
   * Returns the [MediaDescriptionCompat] from the timeline.
   */
  private fun getDescription(player: Player): MediaDescriptionCompat? {
    if (player.currentTimeline.isEmpty) {
      return null
    }

    return player
      .currentTimeline
      .getWindow(player.currentWindowIndex, mWindow, true)
      .tag as MediaDescriptionCompat
  }

  private fun fetchMetadata(source: MediaMetadataCompat): MediaMetadataCompat {
    val metadataBuilder = MediaMetadataCompat.Builder(source).from(source.description)

    val retriever = MediaMetadataRetriever().apply {
      setDataSource(source.id)
    }
    if (retriever.embeddedPicture == null) {
      metadataBuilder.albumArt = placeholderAlbumArt
    }

    return metadataBuilder.build()
  }

  companion object {
    private const val LOG_TAG = "ExoMetadataProvider"
  }
}