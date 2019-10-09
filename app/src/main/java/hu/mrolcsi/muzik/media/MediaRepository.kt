package hu.mrolcsi.muzik.media

import android.support.v4.media.MediaBrowserCompat.MediaItem
import io.reactivex.Observable

interface MediaRepository {

  fun getArtists(): Observable<List<MediaItem>>
  fun getArtistById(artistId: Long): Observable<MediaItem>

  fun getAlbums(): Observable<List<MediaItem>>
  fun getAlbumById(albumId: Long): Observable<MediaItem>
  fun getAlbumsByArtist(artistId: Long): Observable<List<MediaItem>>

  fun getSongs(): Observable<List<MediaItem>>
  fun getSongsByArtist(artistId: Long): Observable<List<MediaItem>>
  fun getSongsFromAlbum(albumId: Long): Observable<List<MediaItem>>

}