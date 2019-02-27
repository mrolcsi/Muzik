package hu.mrolcsi.android.lyricsplayer.extensions

import android.os.Bundle
import android.provider.MediaStore

// Artist related fields

inline val Bundle.artistKey get() = this.getString(MediaStore.Audio.ArtistColumns.ARTIST_KEY) ?: null

inline val Bundle.artist get() = this.getString(MediaStore.Audio.ArtistColumns.ARTIST) ?: null

inline val Bundle.numberOfAlbums get() = this.getInt(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)

inline val Bundle.numberOfTracks get() = this.getInt(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

// Album related fields

inline val Bundle.albumKey get() = this.getString(MediaStore.Audio.AlbumColumns.ALBUM_KEY) ?: null

inline val Bundle.album get() = this.getString(MediaStore.Audio.AlbumColumns.ALBUM) ?: null

inline val Bundle.numberOfSongs get() = this.getInt(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS)

inline val Bundle.albumArtPath get() = this.getString(MediaStore.Audio.AlbumColumns.ALBUM_ART) ?: null

// Song related fields

inline val Bundle.rowId get() = this.getLong(MediaStore.Audio.Media._ID)

inline val Bundle.mediaPath get() = this.getString(MediaStore.Audio.Media.DATA) ?: null

inline val Bundle.title get() = this.getString(MediaStore.Audio.Media.TITLE) ?: null

inline val Bundle.trackNumber get() = this.getInt(MediaStore.Audio.Media.TRACK)