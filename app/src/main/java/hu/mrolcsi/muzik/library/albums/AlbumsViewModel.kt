package hu.mrolcsi.muzik.library.albums

import android.app.Application
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.extensions.switchMap
import hu.mrolcsi.muzik.library.SessionViewModel
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.extensions.media.albumKey
import hu.mrolcsi.muzik.service.extensions.media.artistKey

class AlbumsViewModel(app: Application) : SessionViewModel(app) {

  private val mSubscriptionCallbacks = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      Log.d(getLogTag(), "Items loaded from MediaBrowser: $children")
      mAllAlbums.postValue(children)
    }
  }

  private val mAllAlbums: MutableLiveData<List<MediaBrowserCompat.MediaItem>> by lazy {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>>().also {
      loadAlbums()
    }
  }

  private fun loadAlbums() {
    mMediaBrowser.subscribe(MuzikBrowserService.MEDIA_ROOT_ALBUMS, mSubscriptionCallbacks)
    mMediaBrowser.connect()
  }

  val sorting = MutableLiveData<@SortingMode Int>(SortingMode.SORT_BY_TITLE)

  val albums: LiveData<List<MediaBrowserCompat.MediaItem>>
    get() = sorting.switchMap { sortBy ->
      when (sortBy) {
        null -> mAllAlbums
        SortingMode.SORT_BY_ARTIST -> mAllAlbums.switchMap { albums ->
          val sortedAlbums = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            sortedAlbums.postValue(albums.sortedBy { it.description.artistKey })
          }
          sortedAlbums
        }
        SortingMode.SORT_BY_TITLE -> mAllAlbums.switchMap { albums ->
          val sortedAlbums = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            sortedAlbums.postValue(albums.sortedBy { it.description.albumKey })
          }
          sortedAlbums
        }
        else -> throw IllegalArgumentException("$sortBy cannot be used with albums.")
      }
    }

  override fun getLogTag(): String = "AlbumsViewModel"

}