package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import java.util.concurrent.ExecutorService

/**
 * A wrapper for a [MediaSessionConnector.QueueEditor] implementation that passes callbacks to an [ExecutorService] (preferably a SingleThreadExecutor).
 */
class AsyncQueueEditor(
  private val queueEditor: MediaSessionConnector.QueueEditor,
  private val executor: ExecutorService
) : MediaSessionConnector.QueueEditor, MediaSessionConnector.CommandReceiver {

  // QUEUE EDITOR

  override fun onAddQueueItem(player: Player?, description: MediaDescriptionCompat?) {
    executor.submit {
      queueEditor.onAddQueueItem(player, description)
    }
  }

  override fun onAddQueueItem(player: Player?, description: MediaDescriptionCompat?, index: Int) {
    executor.submit {
      queueEditor.onAddQueueItem(player, description, index)
    }
  }

  override fun onRemoveQueueItem(player: Player?, description: MediaDescriptionCompat?) {
    executor.submit {
      queueEditor.onRemoveQueueItem(player, description)
    }
  }

  // COMMAND RECEIVER

  override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {
    executor.submit {
      queueEditor.onCommand(player, command, extras, cb)
    }
  }

  override fun getCommands(): Array<String> {
    return queueEditor.commands
  }
}