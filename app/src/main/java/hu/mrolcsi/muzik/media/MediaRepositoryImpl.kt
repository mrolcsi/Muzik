package hu.mrolcsi.muzik.media

import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.MuzikBrowserService.Companion.OPTION_ALBUM_ID
import hu.mrolcsi.muzik.service.MuzikBrowserService.Companion.OPTION_ARTIST_ID
import io.reactivex.Observable

class MediaRepositoryImpl constructor(
  private val mediaService: MediaService
) : MediaRepository {

  override fun getArtists(): Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.observableSubscribe(MuzikBrowserService.MEDIA_ROOT_ARTISTS)

  override fun getArtistById(artistId: Long): Observable<MediaBrowserCompat.MediaItem> =
    mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_ARTIST_BY_ID,
      bundleOf(OPTION_ARTIST_ID to artistId)
    ).map { it.single() }

  override fun getAlbums(): Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.observableSubscribe(MuzikBrowserService.MEDIA_ROOT_ALBUMS)

  override fun getAlbumById(albumId: Long): Observable<MediaBrowserCompat.MediaItem> =
    mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_ALBUM_BY_ID,
      bundleOf(OPTION_ALBUM_ID to albumId)
    ).map { it.single() }

  override fun getAlbumsByArtist(artistId: Long): Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_ALBUMS_BY_ARTIST,
      bundleOf(OPTION_ARTIST_ID to artistId)
    )

  override fun getSongs(): Observable<List<MediaBrowserCompat.MediaItem>> =
    mediaService.observableSubscribe(MuzikBrowserService.MEDIA_ROOT_SONGS)

  override fun getSongsByArtist(artistId: Long): Observable<List<MediaBrowserCompat.MediaItem>> {
    return mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_SONGS_BY_ARTIST,
      bundleOf(OPTION_ARTIST_ID to artistId)
    )
  }

  override fun getSongsFromAlbum(albumId: Long): Observable<List<MediaBrowserCompat.MediaItem>> {
    return mediaService.observableSubscribe(
      MuzikBrowserService.MEDIA_ROOT_SONGS_FROM_ALBUM,
      bundleOf(OPTION_ALBUM_ID to albumId)
    )
  }
}