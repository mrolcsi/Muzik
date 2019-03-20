package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.os.AsyncTask
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.from

class ExoMetadataProvider(
  private val mediaController: MediaControllerCompat,
  metadataExtrasPrefix: String? = null,
  private val onCacheUpdated: (MediaMetadataCompat) -> Unit = {}
) : MediaSessionConnector.MediaMetadataProvider {

  private val mDefaultProvider =
    MediaSessionConnector.DefaultMediaMetadataProvider(mediaController, metadataExtrasPrefix)

  private val mMetadataCache = HashMap<String, MediaMetadataCompat?>()

  override fun getMetadata(player: Player): MediaMetadataCompat? {
    // Get description from queue
    val description = getDescription(player) ?: return null

    val mediaId = description.mediaId ?: return null

    // Get metadata from cache
    val cachedMetadata = mMetadataCache[mediaId]

    return if (cachedMetadata != null) {
      Log.v(LOG_TAG, "Item in cache: {${cachedMetadata.description}, hasAlbumArt?=${cachedMetadata.albumArt != null}}")
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
   * Returns the [MediaDescriptionCompat] of the [MediaSessionCompat.QueueItem] of the active queue item.
   */
  private fun getDescription(player: Player): MediaDescriptionCompat? {
    if (player.currentTimeline.isEmpty) {
      return null
    }
    val builder = MediaMetadataCompat.Builder()
    if (player.isPlayingAd) {
      builder.putLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT, 1)
    }
    builder.putLong(
      MediaMetadataCompat.METADATA_KEY_DURATION,
      if (player.duration == C.TIME_UNSET) -1 else player.duration
    )
    val activeQueueItemId = mediaController.playbackState.activeQueueItemId
    if (activeQueueItemId != MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()) {
      val queue = mediaController.queue
      var i = 0
      while (queue != null && i < queue.size) {
        val queueItem = queue[i]
        if (queueItem.queueId == activeQueueItemId) {
          return queueItem.description
        }
        i++
      }
    }
    return null
  }

  private fun fetchMetadata(source: MediaMetadataCompat): MediaMetadataCompat {
    return MediaMetadataCompat.Builder(source)
      .from(source.description)
      .build()
  }

  companion object {
    private const val LOG_TAG = "ExoMetadataProvider"
  }
}