package hu.mrolcsi.android.lyricsplayer.extensions.media

import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat

// Artist related fields

inline val MediaDescriptionCompat.artistKey: String?
  get() = this.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST_KEY)

inline val MediaDescriptionCompat.artist: String?
  get() = this.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST)

inline val MediaDescriptionCompat.numberOfAlbums: Int
  get() = this.extras?.getString(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)?.toInt() ?: 0

inline val MediaDescriptionCompat.numberOfTracks: Int
  get() = this.extras?.getString(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)?.toInt() ?: 0

// Album related fields

inline val MediaDescriptionCompat.albumKey: String?
  get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM_KEY)

inline val MediaDescriptionCompat.album: String?
  get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM)

inline val MediaDescriptionCompat.numberOfSongs: Int
  get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS)?.toInt() ?: 0

inline val MediaDescriptionCompat.albumArtPath: String?
  get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM_ART)

// Song related fields

inline val MediaDescriptionCompat.id: Long
  get() = this.extras?.getString(MediaStore.Audio.Media._ID)?.toLong() ?: -1

inline val MediaDescriptionCompat.mediaPath: String?
  get() = this.extras?.getString(MediaStore.Audio.Media.DATA)

inline val MediaDescriptionCompat.songTitle: String?
  get() = this.extras?.getString(MediaStore.Audio.Media.TITLE)

inline val MediaDescriptionCompat.trackNumber: Int
  get() = this.extras?.getString(MediaStore.Audio.Media.TRACK)?.toInt() ?: 0