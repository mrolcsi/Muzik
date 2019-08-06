package hu.mrolcsi.muzik.media

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.reactivex.Observable

interface MediaService {

  val mediaBrowser: MediaBrowserCompat

  val metadata: Observable<MediaMetadataCompat>
  val playbackState: Observable<PlaybackStateCompat>

  val controller: MediaControllerCompat?

  fun observableSubscribe(parentId: String, options: Bundle? = null): Observable<List<MediaBrowserCompat.MediaItem>>

  fun setQueueTitle(title: CharSequence)

  fun playAll(descriptions: List<MediaDescriptionCompat>, startPosition: Int = 0)
  fun playAllShuffled(descriptions: List<MediaDescriptionCompat>)

  fun seekTo(position: Long)
  fun skipToPrevious()
  fun playPause()
  fun skipToNext()
}