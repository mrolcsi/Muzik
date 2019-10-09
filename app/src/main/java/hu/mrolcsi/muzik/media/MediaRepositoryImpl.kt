package hu.mrolcsi.muzik.media

import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.MuzikBrowserService.Companion.OPTION_ALBUM_ID
import hu.mrolcsi.muzik.service.MuzikBrowserService.Companion.OPTION_ARTIST_ID
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
  private val mediaService: MediaService
) : MediaRepository {

  override fun getArtists(): Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.observableSubscribe(MuzikBrowserService.MEDIA_ROOT_ARTISTS)
      .subscribeOn(Schedulers.io())

  override fun getArtistById(artistId: Long): Observable<MediaBrowserCompat.MediaItem> =
    mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_ARTIST_BY_ID,
      bundleOf(OPTION_ARTIST_ID to artistId)
    )
      .map { it.single() }
      .subscribeOn(Schedulers.io())

  override fun getAlbums(): Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.observableSubscribe(MuzikBrowserService.MEDIA_ROOT_ALBUMS)
      .subscribeOn(Schedulers.io())

  override fun getAlbumById(albumId: Long): Observable<MediaBrowserCompat.MediaItem> =
    mediaService.observableSubscribe(MuzikBrowserService.MEDIA_ROOT_ALBUM_BY_ID, bundleOf(OPTION_ALBUM_ID to albumId))
      .map { it.single() }
      .subscribeOn(Schedulers.io())

  override fun getAlbumsByArtist(artistId: Long): Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_ALBUMS_BY_ARTIST,
      bundleOf(OPTION_ARTIST_ID to artistId)
    ).subscribeOn(Schedulers.io())

  override fun getSongs(): Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.observableSubscribe(MuzikBrowserService.MEDIA_ROOT_SONGS)
      .subscribeOn(Schedulers.io())

  override fun getSongsByArtist(artistId: Long): Observable<List<MediaBrowserCompat.MediaItem>> {
    return mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_SONGS_BY_ARTIST,
      bundleOf(OPTION_ARTIST_ID to artistId)
    ).subscribeOn(Schedulers.io())
  }

  override fun getSongsFromAlbum(albumId: Long): Observable<List<MediaBrowserCompat.MediaItem>> {
    return mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_SONGS_FROM_ALBUM,
      bundleOf(OPTION_ALBUM_ID to albumId)
    ).subscribeOn(Schedulers.io())
  }
}