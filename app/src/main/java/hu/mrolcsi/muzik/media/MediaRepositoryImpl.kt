package hu.mrolcsi.muzik.media

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.muzik.service.MuzikBrowserService
import io.reactivex.Observable
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
  mediaService: MediaService
) : MediaRepository {

  override val artists: Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.subscribeWithObservable(MuzikBrowserService.MEDIA_ROOT_ARTISTS)

  override val albums: Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.subscribeWithObservable(MuzikBrowserService.MEDIA_ROOT_ALBUMS)

  override val songs: Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.subscribeWithObservable(MuzikBrowserService.MEDIA_ROOT_SONGS)

}