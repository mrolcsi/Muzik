package hu.mrolcsi.muzik.media

import android.support.v4.media.MediaBrowserCompat
import io.reactivex.Observable

interface MediaRepository {

  val artists: Observable<List<MediaBrowserCompat.MediaItem>>
  val albums: Observable<List<MediaBrowserCompat.MediaItem>>
  val songs: Observable<List<MediaBrowserCompat.MediaItem>>

}