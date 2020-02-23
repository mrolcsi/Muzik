package hu.mrolcsi.muzik.data.service.media.exoplayer

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor.COMMAND_MOVE_QUEUE_ITEM
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor.EXTRA_FROM_INDEX
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor.EXTRA_TO_INDEX
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.util.Util
import hu.mrolcsi.muzik.BuildConfig
import hu.mrolcsi.muzik.data.service.media.exoplayer.BulkTimelineQueueEditor.OnQueueChangedCallback
import timber.log.Timber

/**
 * An extension class to [TimelineQueueEditor] that also supports *onItemAdded* and *onItemRemoved*
 * operations to be performed in bulk.
 *
 * @param mediaController A [MediaControllerCompat] to read the current queue.
 * @param queueMediaSource The [ConcatenatingMediaSource] to manipulate.
 * @param onQueueChangedCallback A [OnQueueChangedCallback] to get notified when the queue changes.
 * @param sourceFactory The [TimelineQueueEditor.MediaSourceFactory] to build media sources.
 * @param handler An optional [Handler] to process the operations.
 *                It is recommended to use a single background thread.
 *
 * @see TimelineQueueEditor
 */
class BulkTimelineQueueEditor(
  private val mediaController: MediaControllerCompat,
  private val queueMediaSource: ConcatenatingMediaSource,
  private val onQueueChangedCallback: OnQueueChangedCallback,
  private val sourceFactory: TimelineQueueEditor.MediaSourceFactory,
  private val handler: Handler = Handler()
) : MediaSessionConnector.QueueEditor {

  // -- COMMAND RECEIVER --

  override fun onCommand(
    player: Player,
    controlDispatcher: ControlDispatcher?,
    command: String,
    extras: Bundle?,
    cb: ResultReceiver?
  ): Boolean {
    Timber.d("Received command: $command,  Params: $extras")
    return when (command) {
      COMMAND_ADD_QUEUE_ITEMS -> {
        val items = extras!!.getParcelableArrayList<MediaDescriptionCompat>(COMMAND_ARGUMENT_MEDIA_DESCRIPTIONS)!!
        onAddQueueItems(player, items)
        true
      }
      COMMAND_ADD_QUEUE_ITEMS_AT -> {
        val items = extras!!.getParcelableArrayList<MediaDescriptionCompat>(COMMAND_ARGUMENT_MEDIA_DESCRIPTIONS)!!
        val index = extras.getInt(COMMAND_ARGUMENT_INDEX, C.INDEX_UNSET)
        onAddQueueItems(player, items, index)
        true
      }
      COMMAND_REMOVE_QUEUE_ITEMS_RANGE -> {
        val from = extras!!.getInt(COMMAND_ARGUMENT_FROM_INDEX, C.INDEX_UNSET)
        val to = extras.getInt(COMMAND_ARGUMENT_TO_INDEX, C.INDEX_UNSET)
        onRemoveQueueItems(player, from, to)
        true
      }
      COMMAND_CLEAR_QUEUE -> {
        clearQueue()
        true
      }
      COMMAND_MOVE_QUEUE_ITEM -> {
        val from = extras!!.getInt(EXTRA_FROM_INDEX, C.INDEX_UNSET)
        val to = extras.getInt(EXTRA_TO_INDEX, C.INDEX_UNSET)
        if (from != C.INDEX_UNSET && to != C.INDEX_UNSET) {
          onMoveQueueItem(player, from, to)
        }
        true
      }
      else -> false
    }
  }

  // -- QUEUE EDITOR --

  override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat) {
    handler.post {
      val mediaSource = sourceFactory.createMediaSource(description)
      queueMediaSource.addMediaSource(mediaSource, handler) {
        onQueueChangedCallback.onItemAdded(queueMediaSource.size - 1, description)
      }
    }
  }

  override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat, index: Int) {
    handler.post {
      val mediaSource = sourceFactory.createMediaSource(description)
      queueMediaSource.addMediaSource(index, mediaSource, handler) {
        onQueueChangedCallback.onItemAdded(index, description)
      }
    }
  }

  @Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
  fun onAddQueueItems(player: Player, descriptions: Collection<MediaDescriptionCompat>, index: Int = -1) {
    handler.post {
      val mediaSources = descriptions.map { sourceFactory.createMediaSource(it) }
      if (mediaSources.isNotEmpty()) {
        val realIndex = if (index < 0) queueMediaSource.size else index
        queueMediaSource.addMediaSources(realIndex, mediaSources, handler) {
          onQueueChangedCallback.onItemsAdded(realIndex, descriptions)
        }
      }
    }
  }

  override fun onRemoveQueueItem(player: Player, description: MediaDescriptionCompat) {
    handler.post {
      val queue = mediaController.queue
      for (i in queue.indices) {
        if (Util.areEqual(queue[i].description.mediaId, description.mediaId)) {
          queueMediaSource.removeMediaSource(i, handler) {
            onQueueChangedCallback.onItemRemoved(i)
          }
          break
        }
      }
    }
  }

  @Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
  fun onRemoveQueueItems(player: Player, fromIndex: Int, toIndex: Int) {
    handler.post {
      // Range checks
      if (fromIndex < 0 || toIndex > queueMediaSource.size || fromIndex > toIndex) {
        throw IllegalArgumentException()
      } else if (fromIndex != toIndex) {
        // Checking index inequality prevents an unnecessary allocation.
        queueMediaSource.removeMediaSourceRange(fromIndex, toIndex, handler) {
          onQueueChangedCallback.onItemsRemoved(fromIndex, toIndex)
        }
      }
    }
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  fun onMoveQueueItem(player: Player, from: Int, to: Int) {
    handler.post {
      queueMediaSource.moveMediaSource(from, to, handler) {
        onQueueChangedCallback.onItemMoved(from, to)
      }
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun clearQueue() {
    handler.post {
      queueMediaSource.clear(handler) {
        onQueueChangedCallback.onQueueCleared()
      }
    }
  }

  // -- INTERFACES --

  interface OnQueueChangedCallback {

    /**
     * Called when an [MediaDescriptionCompat] was added at the given `position`.
     *
     * @param position The position at which the item was added.
     * @param description The [MediaDescriptionCompat] that was added to the queue.
     */
    fun onItemAdded(position: Int, description: MediaDescriptionCompat)

    /**
     * Called when an item at `position` was removed from the queue.
     *
     * @param position The position the item was removed from.
     */
    fun onItemRemoved(position: Int)

    /**
     * Called when a queue item was moved from position `from` to position `to`.
     *
     * @param from The position from which the item was removed.
     * @param to The target position where the item was moved to.
     */
    fun onItemMoved(from: Int, to: Int)

    /**
     * Called when multiple [MediaDescriptionCompat]s were added at the given `position`.
     *
     * @param position The position at which the item was added.
     * @param descriptions The [MediaDescriptionCompat] collection that was added to the queue.
     */
    fun onItemsAdded(position: Int, descriptions: Collection<MediaDescriptionCompat>)

    /**
     * Called when a range of items where removed starting at `from` and ending at `to`.
     *
     * @param from The first position of the removed items.
     * @param to The last position of the removed items.
     */
    fun onItemsRemoved(from: Int, to: Int)

    /**
     * Called after the whole queue was cleared.
     */
    fun onQueueCleared()
  }

  companion object {
    const val COMMAND_ADD_QUEUE_ITEMS = BuildConfig.APPLICATION_ID + ".ADD_QUEUE_ITEMS"
    const val COMMAND_ADD_QUEUE_ITEMS_AT = BuildConfig.APPLICATION_ID + ".ADD_QUEUE_ITEMS_AT"
    const val COMMAND_REMOVE_QUEUE_ITEMS_RANGE = BuildConfig.APPLICATION_ID + ".REMOVE_QUEUE_ITEMS_RANGE"
    const val COMMAND_CLEAR_QUEUE = BuildConfig.APPLICATION_ID + ".COMMAND_CLEAR_QUEUE"

    const val COMMAND_ARGUMENT_MEDIA_DESCRIPTIONS = BuildConfig.APPLICATION_ID + ".ARGUMENT_MEDIA_DESCRIPTIONS"
    const val COMMAND_ARGUMENT_INDEX = BuildConfig.APPLICATION_ID + ".ARGUMENT_INDEX"
    const val COMMAND_ARGUMENT_FROM_INDEX = BuildConfig.APPLICATION_ID + ".ARGUMENT_FROM_INDEX"
    const val COMMAND_ARGUMENT_TO_INDEX = BuildConfig.APPLICATION_ID + ".ARGUMENT_TO_INDEX"
  }
}