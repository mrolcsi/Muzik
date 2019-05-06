package hu.mrolcsi.muzik.library.songs

import android.app.Application
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.extensions.switchMap
import hu.mrolcsi.muzik.library.SessionViewModel
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.extensions.media.artistKey
import hu.mrolcsi.muzik.service.extensions.media.dateAdded
import hu.mrolcsi.muzik.service.extensions.media.titleKey

class SongsViewModel(app: Application) : SessionViewModel(app) {

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
    mMediaBrowser.subscribe(MuzikBrowserService.MEDIA_ROOT_SONGS, mSubscriptionCallbacks)
    mMediaBrowser.connect()
  }

  val sorting = MutableLiveData<@SortingMode Int>(SortingMode.SORT_BY_TITLE)

  fun getSongs(): LiveData<List<MediaBrowserCompat.MediaItem>> {
    // Switch by album
    return sorting.switchMap { sortBy ->
      when (sortBy) {
        null -> mAllSongs
        SortingMode.SORT_BY_ARTIST -> mAllSongs.switchMap { songs ->
          val sortedSongs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            sortedSongs.postValue(songs.sortedBy { it.description.artistKey })
          }
          sortedSongs
        }
        SortingMode.SORT_BY_TITLE -> mAllSongs.switchMap { songs ->
          val sortedSongs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            sortedSongs.postValue(songs.sortedBy { it.description.titleKey })
          }
          sortedSongs
        }
        SortingMode.SORT_BY_DATE -> mAllSongs.switchMap { songs ->
          val sortedSongs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            sortedSongs.postValue(songs.sortedByDescending { it.description.dateAdded })
          }
          sortedSongs
        }
        else -> throw IllegalArgumentException("unknown sorting constant.")
      }
    }
  }

  override fun getLogTag(): String = "SongsViewModel"

}