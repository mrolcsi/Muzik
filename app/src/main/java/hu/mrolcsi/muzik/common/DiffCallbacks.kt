package hu.mrolcsi.muzik.common

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.recyclerview.widget.DiffUtil
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry

object DiffCallbacks {

  private fun Bundle.isEqualTo(that: Bundle): Boolean {
    if (this.size() != that.size())
      return false

    if (!this.keySet().containsAll(that.keySet()))
      return false

    for (key in this.keySet()) {
      val valueOne = this.get(key)
      val valueTwo = that.get(key)
      if (valueOne is Bundle && valueTwo is Bundle) {
        if (!this.isEqualTo(valueTwo)) return false
      } else if (valueOne != valueTwo) return false
    }

    return true
  }

  val mediaItemCallback = object : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
    override fun areItemsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      return oldItem.mediaId == newItem.mediaId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      // The 'extras' are only equal when both are null.
      return if (oldItem.description.extras == newItem.description.extras) true
      else oldItem.description.extras!!.isEqualTo(newItem.description.extras!!)
    }
  }

  val queueItemCallback = object : DiffUtil.ItemCallback<MediaSessionCompat.QueueItem>() {
    override fun areItemsTheSame(
      oldItem: MediaSessionCompat.QueueItem,
      newItem: MediaSessionCompat.QueueItem
    ): Boolean {
      return oldItem.queueId == newItem.queueId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
      oldItem: MediaSessionCompat.QueueItem,
      newItem: MediaSessionCompat.QueueItem
    ): Boolean {
      // The 'extras' are only equal when both are null.
      return if (oldItem.description.extras == newItem.description.extras) true
      else oldItem.description.extras!!.isEqualTo(newItem.description.extras!!)
    }

  }
  val playQueueEntryCallback = object : DiffUtil.ItemCallback<PlayQueueEntry>() {
    override fun areItemsTheSame(oldItem: PlayQueueEntry, newItem: PlayQueueEntry): Boolean =
      oldItem._data == newItem._data

    override fun areContentsTheSame(oldItem: PlayQueueEntry, newItem: PlayQueueEntry): Boolean = oldItem == newItem
  }

}