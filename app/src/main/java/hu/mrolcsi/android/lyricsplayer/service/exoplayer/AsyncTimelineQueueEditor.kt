package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import java.util.concurrent.Executors

/**
 * A {@link MediaSessionConnector.QueueEditor} implementation based on the {@link
 * ConcatenatingMediaSource}.
 *
 * <p>This class implements the {@link MediaSessionConnector.CommandReceiver} interface and handles
 * the {@link #COMMAND_MOVE_QUEUE_ITEM} to move a queue item instead of removing and inserting it.
 * This allows to move the currently playing window without interrupting playback.
 *
 * As opposed to [TimelineQueueEditor], this implementation performs these tasks in a dedicated background thread.
 */
class AsyncTimelineQueueEditor(
  controller: MediaControllerCompat,
  queueMediaSource: ConcatenatingMediaSource,
  queueDataAdapter: TimelineQueueEditor.QueueDataAdapter,
  sourceFactory: TimelineQueueEditor.MediaSourceFactory
) : MediaSessionConnector.QueueEditor, MediaSessionConnector.CommandReceiver {

  // To keep things sequential
  private val mExecutor = Executors.newSingleThreadExecutor()

  private val mWrappedQueueEditor = TimelineQueueEditor(controller, queueMediaSource, queueDataAdapter, sourceFactory)

  // QUEUE EDITOR

  override fun onAddQueueItem(player: Player?, description: MediaDescriptionCompat?) {
    mExecutor.submit {
      mWrappedQueueEditor.onAddQueueItem(player, description)
    }
  }

  override fun onAddQueueItem(player: Player?, description: MediaDescriptionCompat?, index: Int) {
    mExecutor.submit {
      mWrappedQueueEditor.onAddQueueItem(player, description, index)
    }
  }

  override fun onRemoveQueueItem(player: Player?, description: MediaDescriptionCompat?) {
    mExecutor.submit {
      mWrappedQueueEditor.onRemoveQueueItem(player, description)
    }
  }

  // COMMAND RECEIVER

  override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {
    mExecutor.submit {
      mWrappedQueueEditor.onCommand(player, command, extras, cb)
    }
  }

  override fun getCommands(): Array<String> {
    return mWrappedQueueEditor.commands
  }
}