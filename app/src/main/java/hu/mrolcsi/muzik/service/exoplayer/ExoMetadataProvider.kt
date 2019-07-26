package hu.mrolcsi.muzik.service.exoplayer

import android.content.Context
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.util.LruCache
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import hu.mrolcsi.muzik.service.extensions.media.albumArt
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri

class ExoMetadataProvider(
  private val context: Context,
  mediaController: MediaControllerCompat
) : MediaSessionConnector.MediaMetadataProvider {

  private val mDefaultProvider =
    MediaSessionConnector.DefaultMediaMetadataProvider(mediaController, null)

  private val mMetadataCache = LruCache<String, MediaMetadataCompat>(20)

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
        val newMetadata = fetchBitmap(defaultMetadata)
        Log.v(LOG_TAG, "Updating cache with {${newMetadata.description}}")
        mMetadataCache.put(mediaId, newMetadata)
      }
      Log.v(LOG_TAG, "Using default metadata: {${defaultMetadata.description}}")
      mMetadataCache.put(mediaId, defaultMetadata)
      defaultMetadata
    }
  }

  @Synchronized
  private fun fetchBitmap(source: MediaMetadataCompat): MediaMetadataCompat {
    return try {
      val albumArt = MediaStore.Images.Media.getBitmap(context.contentResolver, source.albumArtUri)
      MediaMetadataCompat.Builder(source)
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
        .build()
    } catch (e: NullPointerException) {
      // MediaStore throws a NullPointerException when the image doesn't exist
      source
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

  companion object {
    private const val LOG_TAG = "ExoMetadataProvider"
  }
}