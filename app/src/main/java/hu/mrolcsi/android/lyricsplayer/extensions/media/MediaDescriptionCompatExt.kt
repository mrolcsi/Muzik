package hu.mrolcsi.android.lyricsplayer.extensions.media

import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat

// Artist related fields

inline val MediaDescriptionCompat.artistKey get() = this.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST_KEY)

inline val MediaDescriptionCompat.artist get() = this.extras?.getString(MediaStore.Audio.ArtistColumns.ARTIST)

inline val MediaDescriptionCompat.numberOfAlbums get() = this.extras?.getInt(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)

inline val MediaDescriptionCompat.numberOfTracks get() = this.extras?.getInt(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

// Album related fields

inline val MediaDescriptionCompat.albumKey get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM_KEY)

inline val MediaDescriptionCompat.album get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM)

inline val MediaDescriptionCompat.numberOfSongs get() = this.extras?.getInt(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS)

inline val MediaDescriptionCompat.albumArtPath get() = this.extras?.getString(MediaStore.Audio.AlbumColumns.ALBUM_ART)

// Song related fields

inline val MediaDescriptionCompat.id get() = this.extras?.getLong(MediaStore.Audio.Media._ID)

inline val MediaDescriptionCompat.mediaPath get() = this.extras?.getString(MediaStore.Audio.Media.DATA)

inline val MediaDescriptionCompat.songTitle get() = this.extras?.getString(MediaStore.Audio.Media.TITLE)

inline val MediaDescriptionCompat.trackNumber get() = this.extras?.getInt(MediaStore.Audio.Media.TRACK)