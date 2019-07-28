package hu.mrolcsi.muzik.media

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.reactivex.Observable

interface MediaService {

  val mediaBrowser: MediaBrowserCompat

  val currentMetadata: Observable<MediaMetadataCompat>
  val currentPlaybackState: Observable<PlaybackStateCompat>

  val mediaController: Observable<MediaControllerCompat>

  fun subscribeWithObservable(parentId: String): Observable<List<MediaBrowserCompat.MediaItem>>
}