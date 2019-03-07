package hu.mrolcsi.android.lyricsplayer.extensions.media

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.BulkTimelineQueueEditor
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoPlayerHolder.Companion.ACTION_START_UPDATER
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoPlayerHolder.Companion.ACTION_STOP_UPDATER

@Suppress("unused")
private const val LOG_TAG = "MediaControllerExt"
private const val MAX_PARCELABLE_SIZE = 300

fun MediaControllerCompat.TransportControls.startProgressUpdater() =
  this.sendCustomAction(ACTION_START_UPDATER, null)

fun MediaControllerCompat.TransportControls.stopProgressUpdater() =
  this.sendCustomAction(ACTION_STOP_UPDATER, null)

fun MediaControllerCompat.clearQueue() =
  this.sendCommand(BulkTimelineQueueEditor.COMMAND_CLEAR_QUEUE, null, null)

fun MediaControllerCompat.addQueueItems(descriptions: Collection<MediaDescriptionCompat>) {
  // Split collection into chunks if too large
  if (descriptions.size > MAX_PARCELABLE_SIZE) {
    descriptions.chunked(MAX_PARCELABLE_SIZE).forEach {
      addQueueItems(it)
    }
  } else {
    // Put parameters into a bundle
    val params = Bundle().apply {
      putParcelableArrayList(
        BulkTimelineQueueEditor.COMMAND_ARGUMENT_MEDIA_DESCRIPTIONS,
        ArrayList(descriptions)
      )
    }
    //Log.d(LOG_TAG, "addQueueItems() Bundle: ${TooLargeTool.bundleBreakdown(params)}")
    this.sendCommand(BulkTimelineQueueEditor.COMMAND_ADD_QUEUE_ITEMS, params, null)
  }
}

fun MediaControllerCompat.addQueueItems(descriptions: Collection<MediaDescriptionCompat>, index: Int) {
  // Split collection into chunks if too large
  if (descriptions.size > MAX_PARCELABLE_SIZE) {
    descriptions.chunked(MAX_PARCELABLE_SIZE).forEachIndexed { i, list ->
      addQueueItems(list, index + i * MAX_PARCELABLE_SIZE)
    }
  } else {
    // Put parameters into a bundle
    val params = Bundle().apply {
      putParcelableArrayList(
        BulkTimelineQueueEditor.COMMAND_ARGUMENT_MEDIA_DESCRIPTIONS,
        ArrayList(descriptions)
      )
      putInt(BulkTimelineQueueEditor.COMMAND_ARGUMENT_INDEX, index)
    }
    this.sendCommand(BulkTimelineQueueEditor.COMMAND_ADD_QUEUE_ITEMS_AT, params, null)
  }
}

fun MediaControllerCompat.removeQueueItems(from: Int, to: Int) {
  // Put parameters into a bundle
  val params = Bundle().apply {
    putInt(BulkTimelineQueueEditor.COMMAND_ARGUMENT_FROM_INDEX, from)
    putInt(BulkTimelineQueueEditor.COMMAND_ARGUMENT_TO_INDEX, to)
  }
  sendCommand(BulkTimelineQueueEditor.COMMAND_REMOVE_QUEUE_ITEMS_RANGE, params, null)
}