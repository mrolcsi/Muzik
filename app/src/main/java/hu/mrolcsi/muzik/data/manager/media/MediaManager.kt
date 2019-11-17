package hu.mrolcsi.muzik.data.manager.media

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.RepeatMode
import android.support.v4.media.session.PlaybackStateCompat.ShuffleMode
import io.reactivex.Observable

interface MediaManager {

  val mediaMetadata: Observable<MediaMetadataCompat>
  val playbackState: Observable<PlaybackStateCompat>
  val repeatMode: Observable<Int>
  val shuffleMode: Observable<Int>
  val queue: Observable<List<QueueItem>>
  val queueTitle: Observable<CharSequence>

  fun getCurrentPlaybackState(): PlaybackStateCompat?
  fun getCurrentMediaMetadata(): MediaMetadataCompat?

  fun observableSubscribe(parentId: String, options: Bundle? = null): Observable<List<MediaBrowserCompat.MediaItem>>

  fun playAll(descriptions: List<MediaDescriptionCompat>, startPosition: Int = 0)
  fun playAllShuffled(descriptions: List<MediaDescriptionCompat>)

  fun seekTo(position: Long)
  fun skipToPrevious()
  fun playPause()
  fun skipToNext()
  fun rewind()
  fun fastForward()

  @ShuffleMode fun getShuffleMode(): Int
  fun setShuffleMode(@ShuffleMode shuffleMode: Int)
  fun toggleShuffle()

  @RepeatMode fun getRepeatMode(): Int
  fun setRepeatMode(@RepeatMode repeatMode: Int)
  fun toggleRepeat()

  fun setQueueTitle(title: CharSequence)
  fun getActiveQueueItemId(): Long
  fun skipToQueueItem(id: Long)
}