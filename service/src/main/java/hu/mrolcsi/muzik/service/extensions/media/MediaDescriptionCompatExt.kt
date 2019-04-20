@file:Suppress("unused")

package hu.mrolcsi.muzik.service.extensions.media

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat

// Try to get ContentProvider columns first. If null, use MediaMetadata keys.

// Artist related fields

inline val MediaDescriptionCompat.artistKey: String?
  get() = this.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST_KEY)

inline val MediaDescriptionCompat.artist: String?
  get() = subtitle?.toString()
    ?: this.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

inline val MediaDescriptionCompat.numberOfAlbums: Int
  get() = this.extras?.getString(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)?.toInt() ?: 0

inline val MediaDescriptionCompat.numberOfTracks: Int
  get() = this.extras?.getString(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)?.toInt() ?: 0

// Album related fields

inline val MediaDescriptionCompat.albumKey: String?
  get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM_KEY)

inline val MediaDescriptionCompat.album: String?
  get() = description?.toString()
    ?: this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)

inline val MediaDescriptionCompat.albumArtist: String?
  get() = this.extras?.getString("album_artist")  // should be a constant
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)

inline val MediaDescriptionCompat.numberOfSongs: Int
  get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS)?.toInt() ?: 0

inline val MediaDescriptionCompat.albumArtUri: Uri
  // This ensures that the file actually gets loaded
  get() = Uri.parse("content://media/external/audio/albumart/$id")

inline val MediaDescriptionCompat.albumArt: Bitmap?
  get() = this.extras?.getParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)

// Song related fields

inline val MediaDescriptionCompat.id: Long
  get() = this.extras?.getString(MediaStore.Audio.Media._ID)?.toLong() ?: -1

inline val MediaDescriptionCompat.mediaPath: String?
  get() = mediaId
    ?: this.extras?.getString(MediaStore.Audio.Media.DATA)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)

inline val MediaDescriptionCompat.songTitle: String?
  get() = this.extras?.getString(MediaStore.Audio.Media.TITLE)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)

inline val MediaDescriptionCompat.coverArtUri: Uri
  get() = Uri.parse("content://media/external/audio/media/$id/albumart")

inline val MediaDescriptionCompat.trackNumber: Int
  get() = this.extras?.getString(MediaStore.Audio.Media.TRACK)?.toInt()
    ?: this.extras?.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)?.toInt()
    ?: -1

inline val MediaDescriptionCompat.duration: Long
  get() = this.extras?.getString(MediaStore.Audio.Media.DURATION)?.toLong()
    ?: this.extras?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
    ?: -1

inline val MediaDescriptionCompat.year: Long
  get() = this.extras?.getString(MediaStore.Audio.Media.YEAR)?.toLong()
    ?: this.extras?.getLong(MediaMetadataCompat.METADATA_KEY_YEAR)
    ?: -1

inline val MediaDescriptionCompat.composer: String?
  get() = this.extras?.getString(MediaStore.Audio.Media.COMPOSER)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_COMPOSER)