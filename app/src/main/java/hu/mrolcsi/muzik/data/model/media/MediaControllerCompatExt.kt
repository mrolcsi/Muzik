package hu.mrolcsi.muzik.data.model.media

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import hu.mrolcsi.muzik.data.service.media.exoplayer.BulkTimelineQueueEditor
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerHolder
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerHolder.Companion.ACTION_PREPARE_FROM_DESCRIPTION
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerHolder.Companion.ACTION_START_UPDATER
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerHolder.Companion.ACTION_STOP_UPDATER
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerHolder.Companion.ARGUMENT_DESCRIPTION
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerHolder.Companion.EXTRA_DESIRED_QUEUE_POSITION

@Suppress("unused")
private const val LOG_TAG = "MediaControllerExt"
private const val MAX_PARCELABLE_SIZE = 300

fun MediaControllerCompat.TransportControls.startProgressUpdater() =
  this.sendCustomAction(ACTION_START_UPDATER, null)

fun MediaControllerCompat.TransportControls.stopProgressUpdater() =
  this.sendCustomAction(ACTION_STOP_UPDATER, null)

fun MediaControllerCompat.prepareFromDescriptions(
  descriptions: List<MediaDescriptionCompat>, startingPosition: Int = 0
) {
  // Load starting item immediately
  val args = Bundle().apply {
    putParcelable(ARGUMENT_DESCRIPTION, descriptions[startingPosition])
    putInt(EXTRA_DESIRED_QUEUE_POSITION, startingPosition)
  }

  // Send action to Service
  this.transportControls.sendCustomAction(ACTION_PREPARE_FROM_DESCRIPTION, args)

  // Add every other song in the background
  AsyncTask.execute {
    this.addQueueItems(
      descriptions.filterIndexed { index, _ -> index != startingPosition }
    )
  }
}

fun MediaControllerCompat.playFromDescriptions(
  descriptions: List<MediaDescriptionCompat>,
  startingPosition: Int = 0
) {
  transportControls.stop()
  prepareFromDescriptions(descriptions, startingPosition)
  transportControls.play()
}

fun MediaControllerCompat.prepareFromMediaItems(
  items: List<MediaBrowserCompat.MediaItem>,
  startingPosition: Int = 0
) {
  // Load starting item
  AsyncTask.execute {
    val first = items[startingPosition]
    val playableItems = items.filter { it.isPlayable }

    val queuePosition = playableItems.indexOf(first)

    prepareFromDescriptions(
      playableItems.map { it.description },
      queuePosition
    )
  }
}

fun MediaControllerCompat.playFromMediaItems(
  items: List<MediaBrowserCompat.MediaItem>,
  startingPosition: Int = 0
) {
  transportControls.stop()
  prepareFromMediaItems(items, startingPosition)
  transportControls.play()
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

fun MediaControllerCompat.setQueueTitle(title: CharSequence) {
  val params = bundleOf(
    ExoPlayerHolder.EXTRA_QUEUE_TITLE to title
  )
  sendCommand(ExoPlayerHolder.ACTION_SET_QUEUE_TITLE, params, null)
}

fun MediaControllerCompat.TransportControls.setShuffleMode(@PlaybackStateCompat.ShuffleMode shuffleMode: Int, seed: Long) {
  val bundle = Bundle().apply {
    putInt(ExoPlayerHolder.EXTRA_SHUFFLE_MODE, shuffleMode)
    putLong(ExoPlayerHolder.EXTRA_SHUFFLE_SEED, seed)
  }
  sendCustomAction(ExoPlayerHolder.ACTION_SET_SHUFFLE_MODE, bundle)
}