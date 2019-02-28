package hu.mrolcsi.android.lyricsplayer.extensions

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerControls.Companion.ACTION_CLEAR_QUEUE
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerControls.Companion.ACTION_START_UPDATER
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerControls.Companion.ACTION_STOP_UPDATER

fun MediaControllerCompat.clearQueue() = this.transportControls.sendCustomAction(ACTION_CLEAR_QUEUE, null)

fun MediaControllerCompat.queueItems(items: List<MediaBrowserCompat.MediaItem>) {
  // Clear current queue
  clearQueue()

  // Add items from adapter to queue
  items.forEachIndexed { index, item ->
    addQueueItem(item.description, index)
  }
}

/**
 * Add [MediaBrowserCompat.MediaItem]s
 * from [items] to the queue, and start playback from [position].
 */
fun MediaControllerCompat.queueItemsAndSkip(items: List<MediaBrowserCompat.MediaItem>, position: Int = 0) {

  queueItems(items)

  // Start media at clicked position.
  // (Assuming adapter position and queue position is the same.)
  transportControls.skipToQueueItem(position.toLong())
}

fun MediaControllerCompat.TransportControls.startProgressUpdater() = this.sendCustomAction(ACTION_START_UPDATER, null)

fun MediaControllerCompat.TransportControls.stopProgressUpdater() = this.sendCustomAction(ACTION_STOP_UPDATER, null)