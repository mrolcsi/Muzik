@file:Suppress("unused")

package hu.mrolcsi.muzik.data.model.media

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import hu.mrolcsi.muzik.data.model.media.MediaType.Companion.MEDIA_TYPE_KEY
import hu.mrolcsi.muzik.data.model.media.MediaType.Companion.MEDIA_UNKNOWN

const val EXTRA_NOW_PLAYING = "android.media.browse.extra.NOW_PLAYING"

// Try to get ContentProvider columns first. If null, use MediaMetadata keys.

@MediaType
inline val MediaDescriptionCompat.type: Int
  get() = this.extras?.getInt(MEDIA_TYPE_KEY) ?: MEDIA_UNKNOWN

inline val MediaDescriptionCompat.album: String?
  get() = description?.toString()
    ?: this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)

inline val MediaDescriptionCompat.albumArt: Bitmap?
  get() = this.extras?.getParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)

inline val MediaDescriptionCompat.albumArtUri: Uri
  get() = Uri.parse("content://media/external/audio/albumart/$id")

inline val MediaDescriptionCompat.albumArtist: String?
  get() = this.extras?.getString("album_artist")  // MediaStore.Audio.AudioColumns.ALBUM_ARTIST
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)

inline val MediaDescriptionCompat.albumId: Long?
  get() = this.extras?.getString(MediaStore.Audio.Media.ALBUM_ID)?.toLong()

inline val MediaDescriptionCompat.albumKey: String?
  get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM_KEY)

inline val MediaDescriptionCompat.albumYear: String?
  get() {
    val firstYear = this.extras?.getString(MediaStore.Audio.Albums.FIRST_YEAR)
    val lastYear = this.extras?.getString(MediaStore.Audio.Albums.LAST_YEAR)
    if (firstYear?.toInt() == 0 || lastYear?.toInt() == 0) return null
    if (firstYear == lastYear) return firstYear
    return "$firstYear - $lastYear"
  }

inline val MediaDescriptionCompat.artist: String?
  get() = subtitle?.toString()
    ?: this.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

inline val MediaDescriptionCompat.artistId: Long?
  get() = this.extras?.getString(MediaStore.Audio.Media.ARTIST_ID)?.toLong()

inline val MediaDescriptionCompat.artistKey: String?
  get() = this.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST_KEY)

inline val MediaDescriptionCompat.composer: String?
  get() = this.extras?.getString(MediaStore.Audio.Media.COMPOSER)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_COMPOSER)

inline val MediaDescriptionCompat.coverArtUri: Uri
  get() = Uri.parse("content://media/external/audio/media/$id/albumart")

inline val MediaDescriptionCompat.dateAdded: Long
  get() = this.extras?.getString(MediaStore.Audio.Media.DATE_ADDED)?.toLong()?.times(1000)
    ?: -1

inline val MediaDescriptionCompat.duration: Long
  get() = this.extras?.getString(MediaStore.Audio.Media.DURATION)?.toLong()
    ?: this.extras?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
    ?: -1

inline val MediaDescriptionCompat.id: Long
  get() = this.extras?.getString(MediaStore.Audio.Media._ID)?.toLong()
    ?: -1

inline val MediaDescriptionCompat.mediaPath: String?
  get() = mediaId
    ?: this.extras?.getString(MediaStore.Audio.Media.DATA)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)

inline val MediaDescriptionCompat.numberOfAlbums: Int
  get() = this.extras?.getString(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)?.toInt()
    ?: 0

inline val MediaDescriptionCompat.numberOfSongs: Int
  get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS)?.toInt()
    ?: 0

inline val MediaDescriptionCompat.numberOfTracks: Int
  get() = this.extras?.getString(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)?.toInt()
    ?: 0

inline val MediaDescriptionCompat.songTitle: String?
  get() = this.extras?.getString(MediaStore.Audio.Media.TITLE)
    ?: this.extras?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)

inline val MediaDescriptionCompat.titleKey: String?
  get() = this.extras?.getString(MediaStore.Audio.Media.TITLE_KEY)

inline val MediaDescriptionCompat.trackNumber: Long
  get() = this.extras?.getString(MediaStore.Audio.Media.TRACK)?.toLong()
    ?: this.extras?.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
    ?: -1L

inline val MediaDescriptionCompat.year: Long
  get() = this.extras?.getString(MediaStore.Audio.Media.YEAR)?.toLong()
    ?: this.extras?.getLong(MediaMetadataCompat.METADATA_KEY_YEAR)
    ?: -1

inline var MediaDescriptionCompat.isNowPlaying: Boolean
  get() = this.extras?.getBoolean(EXTRA_NOW_PLAYING) ?: false
  set(value) {
    this.extras?.putBoolean(EXTRA_NOW_PLAYING, value)
  }