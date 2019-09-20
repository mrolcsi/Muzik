package hu.mrolcsi.muzik.common

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.recyclerview.widget.DiffUtil

object DiffCallbacks {

  private fun MediaBrowserCompat.MediaItem.isEqualTo(that: MediaBrowserCompat.MediaItem): Boolean {
    if (this.mediaId != that.mediaId) return false
    if (this.flags != that.flags) return false
    if (this.isBrowsable != that.isBrowsable) return false
    if (this.isPlayable != that.isPlayable) return false
    if (!this.description.isEqualTo(that.description)) return false

    return true
  }

  private fun MediaDescriptionCompat.isEqualTo(that: MediaDescriptionCompat): Boolean {
    if (this.mediaId != that.mediaId) return false
    if (this.title != that.title) return false
    if (this.subtitle != that.subtitle) return false
    if (this.description != that.description) return false
    if (this.iconBitmap != that.iconBitmap) return false
    if (this.iconUri != that.iconUri) return false
    if (this.mediaUri != that.mediaUri) return false
    if (!this.extras.isEqualTo(that.extras)) return false

    return true
  }

  private fun Bundle?.isEqualTo(that: Bundle?): Boolean {
    when {
      this == null && that == null -> return true
      this != null && that == null -> return false
      this == null && that != null -> return false
      this != null && that != null -> {
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
      }
    }
    return true
  }

  private fun MediaSessionCompat.QueueItem.isEqualTo(that: MediaSessionCompat.QueueItem): Boolean {
    if (this.queueId != that.queueId) return false
    if (!this.description.isEqualTo(that.description)) return false

    return true
  }

  val mediaItemCallback = object : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
    override fun areItemsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      return oldItem.mediaId == newItem.mediaId
    }

    override fun areContentsTheSame(
      oldItem: MediaBrowserCompat.MediaItem,
      newItem: MediaBrowserCompat.MediaItem
    ): Boolean {
      //return oldItem.isEqualTo(newItem)
      return false
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
      //return oldItem.isEqualTo(newItem)
      return false
    }

  }

}