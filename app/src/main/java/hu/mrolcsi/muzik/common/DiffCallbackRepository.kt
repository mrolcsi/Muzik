package hu.mrolcsi.muzik.common

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.recyclerview.widget.DiffUtil
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry

object DiffCallbackRepository {

  val mediaItemCallback = object : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
    override fun areItemsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      return oldItem == newItem
    }

    override fun areContentsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      return oldItem.description.mediaId == newItem.description.mediaId
    }
  }

  val queueItemCallback = object : DiffUtil.ItemCallback<MediaSessionCompat.QueueItem>() {
    override fun areItemsTheSame(
      oldItem: MediaSessionCompat.QueueItem,
      newItem: MediaSessionCompat.QueueItem
    ): Boolean {
      return oldItem.queueId == newItem.queueId
    }

    override fun areContentsTheSame(
      oldItem: MediaSessionCompat.QueueItem,
      newItem: MediaSessionCompat.QueueItem
    ): Boolean {
      return oldItem.description.mediaId == newItem.description.mediaId
    }

  }
  val playQueueEntryCallback = object : DiffUtil.ItemCallback<PlayQueueEntry>() {
    override fun areItemsTheSame(oldItem: PlayQueueEntry, newItem: PlayQueueEntry): Boolean =
      oldItem._data == newItem._data

    override fun areContentsTheSame(oldItem: PlayQueueEntry, newItem: PlayQueueEntry): Boolean = oldItem == newItem
  }

}