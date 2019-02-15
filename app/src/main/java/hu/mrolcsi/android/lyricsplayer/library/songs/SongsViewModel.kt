package hu.mrolcsi.android.lyricsplayer.library.songs

import android.app.Application
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.android.lyricsplayer.library.LibraryViewModel
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService

class SongsViewModel(app: Application) : LibraryViewModel(app) {

  private val mSubscriptionCallbacks = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      Log.d(this@SongsViewModel.toString(), "Items loaded from MediaBrowser: $children")
      mAllSongs = children
      mSongs.postValue(mAllSongs)
    }
  }

  private var mAllSongs = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

  private val mSongs: MutableLiveData<List<MediaBrowserCompat.MediaItem>> = MutableLiveData()

  init {
    loadSongs()
  }

  private fun loadSongs() {
    mMediaBrowser.subscribe(LPBrowserService.MEDIA_ALBUMS_ID, mSubscriptionCallbacks)
    mMediaBrowser.connect()
  }

  fun getSongs(): LiveData<List<MediaBrowserCompat.MediaItem>> {
    return mSongs
  }

  fun clearFilter() {
    mSongs.postValue(mAllSongs)
  }

  fun filterByAlbum(albumKey: String?) {
    AsyncTask.execute {
      val filteredSongs = mAllSongs.filter { item ->
        item.description.extras?.getString(MediaStore.Audio.Media.ALBUM_KEY) == albumKey
      }.sortedBy { item ->
        item.description.extras?.getInt(MediaStore.Audio.Media.TRACK)
      }
      mSongs.postValue(filteredSongs)
    }
  }

  fun filterByArtist(artistKey: String?) {
    AsyncTask.execute {
      val filteredSongs = mAllSongs.filter { item ->
        item.description.extras?.getString(MediaStore.Audio.Media.ARTIST_KEY) == artistKey
      }
      mSongs.postValue(filteredSongs)
    }
  }
}