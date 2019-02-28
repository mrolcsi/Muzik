package hu.mrolcsi.android.lyricsplayer.service

import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.ContentResolverCompat
import androidx.core.os.bundleOf
import androidx.media.MediaBrowserServiceCompat
import hu.mrolcsi.android.lyricsplayer.BuildConfig

abstract class LPBrowserService : MediaBrowserServiceCompat() {

  override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
    // (Optional) Control the level of access for the specified package name.
    // You'll need to write your own logic to do this.
    return if (allowBrowsing(clientPackageName, clientUid)) {
      // Returns a root ID that clients can use with onLoadChildren() to retrieve
      // the content hierarchy.
      MediaBrowserServiceCompat.BrowserRoot(MEDIA_ROOT_ID, null)
    } else {
      // Clients can connect, but this BrowserRoot is an empty hierarchy
      // so onLoadChildren returns nothing. This disables the ability to browse for content.
      MediaBrowserServiceCompat.BrowserRoot(EMPTY_MEDIA_ROOT_ID, null)
    }
  }

  override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
    //  Browsing not allowed
    if (parentId == EMPTY_MEDIA_ROOT_ID) {
      result.sendResult(null)
      return
    }

    // do loading in the background
    AsyncTask.execute {
      val mediaItems = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

      when (parentId) {
        MEDIA_ARTISTS_ID -> {
          mediaItems.addAll(gatherArtists(MediaStore.Audio.Artists.INTERNAL_CONTENT_URI))
          mediaItems.addAll(gatherArtists(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI))

          mediaItems.sortBy { it.mediaId }
        }
        MEDIA_ALBUMS_ID -> {
          mediaItems.addAll(gatherAlbums(MediaStore.Audio.Albums.INTERNAL_CONTENT_URI))
          mediaItems.addAll(gatherAlbums(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI))

          mediaItems.sortBy { it.mediaId }
        }
        MEDIA_SONGS_ID -> {
          mediaItems.addAll(gatherSongs(MediaStore.Audio.Media.INTERNAL_CONTENT_URI))
          mediaItems.addAll(gatherSongs(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI))

          mediaItems.sortBy { it.mediaId }
        }
      }

      result.sendResult(mediaItems)
    }

    // Let it load in the background.
    result.detach()
  }

  private fun gatherArtists(uri: Uri): List<MediaBrowserCompat.MediaItem> {
    val cursorWithArtists = ContentResolverCompat.query(
      contentResolver,
      uri,
      arrayOf(
        MediaStore.Audio.ArtistColumns.ARTIST_KEY,
        MediaStore.Audio.ArtistColumns.ARTIST,
        MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS,
        MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS
      ),
      null,
      null,
      MediaStore.Audio.ArtistColumns.ARTIST_KEY,
      null
    )

    val mediaItems = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

    cursorWithArtists.use {
      while (it.moveToNext()) {
        // Skip Unknown artists (system sounds and such).
        if (cursorWithArtists.getString(1) == MediaStore.UNKNOWN_STRING) continue

        val description = MediaDescriptionCompat.Builder()
          .setMediaId(it.getString(0))      // Artist Key
          .setTitle(it.getString(1))        // Artist Name
          .setExtras(
            bundleOf(
              MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS to it.getInt(2),
              MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS to it.getInt(3)
            )
          )
          .build()

        mediaItems.add(
          MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
          )
        )
      }
    }

    return mediaItems
  }

  private fun gatherAlbums(uri: Uri): List<MediaBrowserCompat.MediaItem> {
    val cursorWithAlbums = ContentResolverCompat.query(
      contentResolver,
      uri,
      arrayOf(
        MediaStore.Audio.AlbumColumns.ALBUM_KEY,
        MediaStore.Audio.AlbumColumns.ALBUM,
        MediaStore.Audio.AlbumColumns.ARTIST,
        MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS,
        MediaStore.Audio.AlbumColumns.ALBUM_ART
      ),
      null,
      null,
      MediaStore.Audio.AlbumColumns.ALBUM_KEY,
      null
    )

    val mediaItems = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

    cursorWithAlbums.use {
      while (it.moveToNext()) {

        val description = MediaDescriptionCompat.Builder()
          .setMediaId(it.getString(0))    // Album key
          .setTitle(it.getString(1))      // Album title
          .setSubtitle(it.getString(2))   // Album artist
          .setExtras(
            bundleOf(
              MediaStore.Audio.AlbumColumns.ALBUM_KEY to it.getString(0),
              MediaStore.Audio.AlbumColumns.ALBUM to it.getString(1),
              MediaStore.Audio.AlbumColumns.ARTIST to it.getString(2),
              MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS to it.getInt(3),
              MediaStore.Audio.AlbumColumns.ALBUM_ART to it.getString(4)
            )
          )
          .build()

        mediaItems.add(
          MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
          )
        )
      }
    }

    return mediaItems
  }

  private fun gatherSongs(uri: Uri): List<MediaBrowserCompat.MediaItem> {
    val cursorWithSongs = ContentResolverCompat.query(
      contentResolver,
      uri,
      arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ARTIST_KEY,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_KEY,
        MediaStore.Audio.Media.TRACK
      ),
      "${MediaStore.Audio.Media.IS_MUSIC} = ?",
      arrayOf("1"),
      MediaStore.Audio.Media.TITLE_KEY,
      null
    )

    val mediaItems = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

    cursorWithSongs.use {
      while (it.moveToNext()) {

        val description = MediaDescriptionCompat.Builder()
          .setMediaId(it.getString(1))    // Song path
          .setTitle(it.getString(2))      // Song title
          .setSubtitle(it.getString(3))   // Song artist
          .setExtras(
            bundleOf(
              MediaStore.Audio.Media._ID to it.getLong(0),
              MediaStore.Audio.Media.ARTIST to it.getString(3),
              MediaStore.Audio.Media.ARTIST_KEY to it.getString(4),
              MediaStore.Audio.Media.ALBUM to it.getString(5),
              MediaStore.Audio.Media.ALBUM_KEY to it.getString(6),
              MediaStore.Audio.Media.TRACK to it.getInt(7).rem(1000)
            )
          )
          .build()

        mediaItems.add(
          MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
          )
        )
      }
    }

    return mediaItems
  }

  // --------

  private fun allowBrowsing(clientPackageName: String, clientUid: Int): Boolean {
    return clientPackageName.startsWith(BuildConfig.APPLICATION_ID)
  }

  companion object {
    private const val LOG_TAG = "LPBrowserService"
    private const val MEDIA_ROOT_ID = LPBrowserService.MEDIA_ARTISTS_ID
    private const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"

    const val MEDIA_ARTISTS_ID = "media_artists"
    const val MEDIA_ALBUMS_ID = "media_albums"
    const val MEDIA_SONGS_ID = "media_songs"
  }
}
