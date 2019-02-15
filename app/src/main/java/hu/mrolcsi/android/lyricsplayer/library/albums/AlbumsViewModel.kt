package hu.mrolcsi.android.lyricsplayer.library.albums

import android.app.Application
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.library.LibraryViewModel
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService

class AlbumsViewModel(app: Application) : LibraryViewModel(app) {

  // TODO: do filtering through Transformation.map?

  private val mSubscriptionCallbacks = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      Log.d(this@AlbumsViewModel.toString(), "Items loaded from MediaBrowser: $children")
      mAllAlbums = children
      mAlbums.postValue(mAllAlbums)
    }
  }

  private var mAllAlbums = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

  private val mAlbums: MutableLiveData<List<MediaBrowserCompat.MediaItem>> = MutableLiveData()

  init {
    loadAlbums()
  }

  private fun loadAlbums() {
    mMediaBrowser.subscribe(LPBrowserService.MEDIA_ALBUMS_ID, mSubscriptionCallbacks)
    mMediaBrowser.connect()
  }

  fun getAlbums(): LiveData<List<MediaBrowserCompat.MediaItem>> {
    return mAlbums
  }

  fun clearFilter() {
    AsyncTask.execute {
      mAlbums.postValue(mAllAlbums)
    }
  }

  fun filterByArtist(artistKey: String?, artistName: String?, numberOfTracks: Int) {
    AsyncTask.execute {
      // Filter albums by artist
      val filteredAlbums = mAllAlbums.filter { item ->
        item.description.extras?.getString(MediaStore.Audio.Albums.ARTIST) == artistName
      }.toMutableList()
      // Add "All songs" as first item
      val allSongsItem = MediaBrowserCompat.MediaItem(
        MediaDescriptionCompat.Builder()
          .setMediaId(AlbumsAdapter.MEDIA_ID_ALL_SONGS)
          .setTitle(getApplication<Application>().getString(R.string.albums_showAllSongs))
          .setSubtitle(
            getApplication<Application>().resources.getQuantityString(
              R.plurals.artists_numberOfSongs,
              numberOfTracks,
              numberOfTracks
            )
          )
          .setExtras(Bundle().apply {
            putString(MediaStore.Audio.ArtistColumns.ARTIST_KEY, artistKey)
            putString(MediaStore.Audio.ArtistColumns.ARTIST, artistName)
          })
          .build(),
        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
      )
      filteredAlbums.add(0, allSongsItem)
      // Post list to UI
      mAlbums.postValue(filteredAlbums)
    }
  }

}