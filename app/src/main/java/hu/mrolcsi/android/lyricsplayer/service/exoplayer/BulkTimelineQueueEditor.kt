package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.BulkTimelineQueueEditor.QueueDataAdapter

// TODO: copy docs from [TimelineQueueEditor]
// TODO: create a wholly new class based on [TimelineQueueEditor]

/**
 * An extension class to [TimelineQueueEditor] that also supports *add* and *remove*
 * operations to be performed in bulk.
 *
 * @param mediaController A [MediaControllerCompat] to read the current queue.
 * @param queueMediaSource The [ConcatenatingMediaSource] to manipulate.
 * @param queueDataAdapter A [QueueDataAdapter] to change the backing data.
 * @param sourceFactory The [TimelineQueueEditor.MediaSourceFactory] to build media sources.
 * @param handler An optional [Handler] to process the operations.
 *
 * @see TimelineQueueEditor
 */
class BulkTimelineQueueEditor(
  private val mediaController: MediaControllerCompat,
  private val queueMediaSource: ConcatenatingMediaSource,
  private val queueDataAdapter: QueueDataAdapter,
  private val sourceFactory: TimelineQueueEditor.MediaSourceFactory,
  private val handler: Handler = Handler()
) : MediaSessionConnector.QueueEditor, MediaSessionConnector.CommandReceiver {

  private val mInternalEditor: TimelineQueueEditor =
    TimelineQueueEditor(mediaController, queueMediaSource, queueDataAdapter, sourceFactory)

  // -- COMMAND RECEIVER --

  override fun getCommands(): Array<String> {
    return mInternalEditor.commands +
        COMMAND_ADD_QUEUE_ITEMS +
        COMMAND_ADD_QUEUE_ITEMS_AT +
        COMMAND_REMOVE_QUEUE_ITEMS_RANGE +
        COMMAND_CLEAR_QUEUE
  }

  override fun onCommand(player: Player, command: String, extras: Bundle?, cb: ResultReceiver?) {
    Log.d(LOG_TAG, "Received command: $command,  Params: $extras")
    when (command) {
      COMMAND_ADD_QUEUE_ITEMS -> {
        val items = extras!!.getParcelableArrayList<MediaDescriptionCompat>(COMMAND_ARGUMENT_MEDIA_DESCRIPTIONS)!!
        onAddQueueItems(player, items)
      }
      COMMAND_ADD_QUEUE_ITEMS_AT -> {
        val items = extras!!.getParcelableArrayList<MediaDescriptionCompat>(COMMAND_ARGUMENT_MEDIA_DESCRIPTIONS)!!
        val index = extras.getInt(COMMAND_ARGUMENT_INDEX, C.INDEX_UNSET)
        onAddQueueItems(player, items, index)
      }
      COMMAND_REMOVE_QUEUE_ITEMS_RANGE -> {
        val from = extras!!.getInt(COMMAND_ARGUMENT_FROM_INDEX, C.INDEX_UNSET)
        val to = extras.getInt(COMMAND_ARGUMENT_TO_INDEX, C.INDEX_UNSET)
        onRemoveQueueItems(player, from, to)
      }
      COMMAND_CLEAR_QUEUE -> clearQueue()
      else -> mInternalEditor.onCommand(player, command, extras, cb)
    }
  }

  // -- QUEUE EDITOR --

  override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat) {
    handler.post {
      mInternalEditor.onAddQueueItem(player, description)
    }
  }

  override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat, index: Int) {
    handler.post {
      mInternalEditor.onAddQueueItem(player, description, index)
    }
  }

  override fun onRemoveQueueItem(player: Player, description: MediaDescriptionCompat) {
    handler.post {
      mInternalEditor.onRemoveQueueItem(player, description)
    }
  }

  // -- CUSTOM IMPLEMENTATIONS --

  @Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
  fun onAddQueueItems(
    player: Player,
    descriptions: Collection<MediaDescriptionCompat>,
    index: Int = -1,
    completionHandler: Handler? = null,
    onCompletionAction: (() -> Unit)? = null
  ) {
    handler.post {
      val mediaSources = descriptions.map { sourceFactory.createMediaSource(it) }
      if (mediaSources.isNotEmpty()) {
        val realIndex = if (index < 0) queueMediaSource.size else index
        queueMediaSource.addMediaSources(realIndex, mediaSources, completionHandler ?: handler) {
          onCompletionAction?.invoke()
          queueDataAdapter.onItemsAdded(realIndex, descriptions)
        }
      }
    }
  }

  @Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
  fun onRemoveQueueItems(
    player: Player,
    fromIndex: Int,
    toIndex: Int,
    completionHandler: Handler? = null,
    onCompletionAction: (() -> Unit)? = null
  ) {
    handler.post {
      // Range checks
      if (fromIndex < 0 || toIndex > queueMediaSource.size || fromIndex > toIndex) {
        throw IllegalArgumentException()
      } else if (fromIndex != toIndex) {
        // Checking index inequality prevents an unnecessary allocation.
        queueMediaSource.removeMediaSourceRange(fromIndex, toIndex, completionHandler ?: handler) {
          onCompletionAction?.invoke()
          queueDataAdapter.onItemsRemoved(fromIndex, toIndex)
        }
      }
    }
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  fun onMoveQueueItem(
    player: Player,
    from: Int,
    to: Int,
    completionHandler: Handler? = null,
    onCompletionAction: (() -> Unit)? = null
  ) {
    queueMediaSource.moveMediaSource(from, to, completionHandler ?: handler) {
      onCompletionAction?.invoke()
      queueDataAdapter.move(from, to)
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun clearQueue(
    completionHandler: Handler? = null,
    onCompletionAction: (() -> Unit)? = null
  ) {
    handler.post {
      queueMediaSource.clear(completionHandler ?: handler) {
        queueDataAdapter.onClear()
        onCompletionAction?.invoke()
      }
    }
  }

  // -- INTERFACES --

  interface QueueDataAdapter : TimelineQueueEditor.QueueDataAdapter {

    fun onItemsAdded(position: Int, descriptions: Collection<MediaDescriptionCompat>)

    fun onItemsRemoved(from: Int, to: Int)

    fun onClear()
  }

  companion object {
    private const val LOG_TAG = "BulkTimelineQueueEditor"

    const val COMMAND_ADD_QUEUE_ITEMS = "hu.mrolcsi.lyricsplayer.service.ADD_QUEUE_ITEMS"
    const val COMMAND_ADD_QUEUE_ITEMS_AT = "hu.mrolcsi.lyricsplayer.service.ADD_QUEUE_ITEMS_AT"
    const val COMMAND_REMOVE_QUEUE_ITEMS_RANGE = "hu.mrolcsi.lyricsplayer.service.REMOVE_QUEUE_ITEMS_RANGE"
    const val COMMAND_CLEAR_QUEUE = "hu.mrolcsi.lyricsplayer.service.COMMAND_CLEAR_QUEUE"

    const val COMMAND_ARGUMENT_MEDIA_DESCRIPTIONS = "hu.mrolcsi.lyricsplayer.service.ARGUMENT_MEDIA_DESCRIPTIONS"
    const val COMMAND_ARGUMENT_INDEX = "hu.mrolcsi.lyricsplayer.service.ARGUMENT_INDEX"
    const val COMMAND_ARGUMENT_FROM_INDEX = "hu.mrolcsi.lyricsplayer.service.ARGUMENT_FROM_INDEX"
    const val COMMAND_ARGUMENT_TO_INDEX = "hu.mrolcsi.lyricsplayer.service.ARGUMENT_TO_INDEX"
  }
}