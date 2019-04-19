package hu.mrolcsi.muzik.service

import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.ContentResolverCompat
import androidx.media.MediaBrowserServiceCompat

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
      null,
      null,
      null,
      MediaStore.Audio.ArtistColumns.ARTIST_KEY,
      null
    )

    val mediaItems = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

    val indexArtistKey = cursorWithArtists.getColumnIndex(MediaStore.Audio.ArtistColumns.ARTIST_KEY)
    val indexArtistName = cursorWithArtists.getColumnIndex(MediaStore.Audio.ArtistColumns.ARTIST)

    cursorWithArtists.use {
      while (it.moveToNext()) {
        // Skip Unknown artists (system sounds and such).
        if (cursorWithArtists.getString(indexArtistName) == MediaStore.UNKNOWN_STRING) continue

        val description = MediaDescriptionCompat.Builder()
          .setMediaId(it.getString(indexArtistKey))      // Artist Key
          .setTitle(it.getString(indexArtistName))        // Artist Name
          .setExtras(createExtras(cursorWithArtists))
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
      null,
      null,
      null,
      MediaStore.Audio.AlbumColumns.ALBUM_KEY,
      null
    )

    val mediaItems = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

    val indexAlbumKey = cursorWithAlbums.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM_KEY)
    val indexAlbumTitle = cursorWithAlbums.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM)
    val indexAlbumArtist = cursorWithAlbums.getColumnIndex(MediaStore.Audio.AlbumColumns.ARTIST)

    cursorWithAlbums.use {
      while (it.moveToNext()) {

        val description = MediaDescriptionCompat.Builder()
          .setMediaId(it.getString(indexAlbumKey))    // Album key
          .setTitle(it.getString(indexAlbumTitle))      // Album title
          .setSubtitle(it.getString(indexAlbumArtist))   // Album artist
          .setExtras(createExtras(cursorWithAlbums))
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
      null,
      "${MediaStore.Audio.Media.IS_MUSIC} = ?",
      arrayOf("1"),
      MediaStore.Audio.Media.TITLE_KEY,
      null
    )

    val mediaItems = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

    val indexPath = cursorWithSongs.getColumnIndex(MediaStore.Audio.Media.DATA)
    val indexTitle = cursorWithSongs.getColumnIndex(MediaStore.Audio.Media.TITLE)
    val indexArtist = cursorWithSongs.getColumnIndex(MediaStore.Audio.Media.ARTIST)
    val indexAlbum = cursorWithSongs.getColumnIndex(MediaStore.Audio.Media.ALBUM)

    cursorWithSongs.use {
      while (it.moveToNext()) {

        // Skip system sounds
        if (it.getString(1).startsWith("/system")) {
          continue
        }

        val description = MediaDescriptionCompat.Builder()
          .setMediaId(it.getString(indexPath))    // Song path
          .setTitle(it.getString(indexTitle))      // Song title
          .setSubtitle(it.getString(indexArtist))   // Song artist
          .setDescription(it.getString(indexAlbum))   // Song album
          .setExtras(createExtras(cursorWithSongs))
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

  /**
   * Put every column into a bundle.
   */
  private fun createExtras(cursor: Cursor): Bundle {
    val bundle = Bundle()
    for (i in 0 until cursor.columnCount) {
      bundle.putString(
        cursor.columnNames[i],
        cursor.getString(i)
      )
    }
    return bundle
  }

  // --------

  private fun allowBrowsing(clientPackageName: String, clientUid: Int): Boolean {
    return clientPackageName.startsWith(BuildConfig.APPLICATION_ID)
  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "LPBrowserService"

    private const val MEDIA_ROOT_ID = LPBrowserService.MEDIA_ARTISTS_ID
    private const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"

    const val MEDIA_ARTISTS_ID = "media_artists"
    const val MEDIA_ALBUMS_ID = "media_albums"
    const val MEDIA_SONGS_ID = "media_songs"
  }
}
