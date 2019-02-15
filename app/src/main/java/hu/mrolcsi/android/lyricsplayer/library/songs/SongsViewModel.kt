package hu.mrolcsi.android.lyricsplayer.library.songs

import android.app.Application
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.android.lyricsplayer.extensions.switchMap
import hu.mrolcsi.android.lyricsplayer.library.LibraryViewModel
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService

class SongsViewModel(app: Application) : LibraryViewModel(app) {

  private val mSubscriptionCallbacks = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      mAllSongs.postValue(children)
    }
  }

  private val mAllSongs: MutableLiveData<List<MediaBrowserCompat.MediaItem>> by lazy {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>>().also {
      loadSongs()
    }
  }

  private fun loadSongs() {
    mMediaBrowser.subscribe(LPBrowserService.MEDIA_SONGS_ID, mSubscriptionCallbacks)
    mMediaBrowser.connect()
  }

  fun getSongs(): LiveData<List<MediaBrowserCompat.MediaItem>> {
    // Switch by album
    return songFilter.switchMap { filter ->
      when (filter.albumKey) {
        null -> when (filter.artistKey) {
          null -> mAllSongs
          else -> mAllSongs.switchMap { allSongs ->
            val songsByArtist = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
            AsyncTask.execute {
              songsByArtist.postValue(filterByArtist(allSongs, filter.artistKey))
            }
            songsByArtist
          }
        }
        else -> mAllSongs.switchMap { allSongs ->
          val songsFromAlbum = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            songsFromAlbum.postValue(filterByAlbum(allSongs, filter.albumKey))
          }
          songsFromAlbum
        }
      }
    }
  }

  val songFilter = MutableLiveData<SongFilter>()

  private fun filterByAlbum(
    allSongs: List<MediaBrowserCompat.MediaItem>,
    albumKey: String?
  ): List<MediaBrowserCompat.MediaItem> {
    return allSongs.filter { item ->
      item.description.extras?.getString(MediaStore.Audio.Media.ALBUM_KEY) == albumKey
    }.sortedBy { item ->
      item.description.extras?.getInt(MediaStore.Audio.Media.TRACK)
    }
  }

  private fun filterByArtist(
    allSongs: List<MediaBrowserCompat.MediaItem>,
    artistKey: String?
  ): List<MediaBrowserCompat.MediaItem> {
    return allSongs.filter { item ->
      item.description.extras?.getString(MediaStore.Audio.Media.ARTIST_KEY) == artistKey
    }
  }

  data class SongFilter(
    val artistKey: String?,
    val albumKey: String?
  )
}