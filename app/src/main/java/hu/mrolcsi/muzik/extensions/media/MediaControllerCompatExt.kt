package hu.mrolcsi.muzik.extensions.media

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import hu.mrolcsi.muzik.service.exoplayer.BulkTimelineQueueEditor
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder.Companion.ACTION_PLAY_FROM_DESCRIPTION
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder.Companion.ACTION_PREPARE_FROM_DESCRIPTION
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder.Companion.ACTION_START_UPDATER
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder.Companion.ACTION_STOP_UPDATER
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder.Companion.ARGUMENT_DESCRIPTION

@Suppress("unused")
private const val LOG_TAG = "MediaControllerExt"
private const val MAX_PARCELABLE_SIZE = 300

fun MediaControllerCompat.TransportControls.startProgressUpdater() =
  this.sendCustomAction(ACTION_START_UPDATER, null)

fun MediaControllerCompat.TransportControls.stopProgressUpdater() =
  this.sendCustomAction(ACTION_STOP_UPDATER, null)

fun MediaControllerCompat.TransportControls.prepareFromDescription(
  description: MediaDescriptionCompat,
  extras: Bundle?
) {
  // Gather parameters
  val args = Bundle().apply {
    putParcelable(ARGUMENT_DESCRIPTION, description)
  }
  if (extras != null) args.putAll(extras)

  // Send action to Service
  this.sendCustomAction(ACTION_PREPARE_FROM_DESCRIPTION, args)
}

fun MediaControllerCompat.TransportControls.playFromDescription(
  description: MediaDescriptionCompat,
  extras: Bundle?
) {
  // Gather parameters
  val args = Bundle().apply {
    putBoolean(ACTION_PLAY_FROM_DESCRIPTION, true)
  }
  if (extras != null) args.putAll(extras)

  // Call prepare
  prepareFromDescription(description, args)
}

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

fun MediaControllerCompat.moveQueueItem(from: Int, to: Int) {
  // Put parameters into a bundle
  val params = bundleOf(
    BulkTimelineQueueEditor.COMMAND_ARGUMENT_FROM_INDEX to from,
    BulkTimelineQueueEditor.COMMAND_ARGUMENT_TO_INDEX to to
  )
  sendCommand(TimelineQueueEditor.COMMAND_MOVE_QUEUE_ITEM, params, null)
}

fun MediaControllerCompat.TransportControls.setShuffleMode(@PlaybackStateCompat.ShuffleMode shuffleMode: Int, seed: Long) {
  val bundle = Bundle().apply {
    putInt(ExoPlayerHolder.EXTRA_SHUFFLE_MODE, shuffleMode)
    putLong(ExoPlayerHolder.EXTRA_SHUFFLE_SEED, seed)
  }
  sendCustomAction(ExoPlayerHolder.ACTION_SET_SHUFFLE_MODE, bundle)
}