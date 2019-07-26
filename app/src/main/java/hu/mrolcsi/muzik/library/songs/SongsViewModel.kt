package hu.mrolcsi.muzik.library.songs

import android.app.Application
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.extensions.switchMap
import hu.mrolcsi.muzik.library.SessionViewModelBase
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.artistKey
import hu.mrolcsi.muzik.service.extensions.media.dateAdded
import hu.mrolcsi.muzik.service.extensions.media.titleKey

class SongsViewModel(app: Application) : SessionViewModelBase(app) {

  private val mShuffleItem = MediaBrowserCompat.MediaItem(
    MediaDescriptionCompat.Builder()
      .setMediaId("shuffle/all")
      .setTitle(app.getString(R.string.mediaControl_shuffleAll))
      .setIconBitmap(BitmapFactory.decodeResource(app.resources, R.drawable.ic_shuffle))
      .setExtras(bundleOf(MediaType.MEDIA_TYPE_KEY to MediaType.MEDIA_OTHER))
      .build(),
    0
  )

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
  }

  val sorting = MutableLiveData<@SortingMode Int>(SortingMode.SORT_BY_TITLE)

  val songs: LiveData<List<MediaBrowserCompat.MediaItem>>
    get() = sorting.switchMap { sortBy ->
      when (sortBy) {
        SortingMode.SORT_BY_ARTIST -> mAllSongs.switchMap { songs ->
          val sortedSongs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            val items = songs.sortedBy { it.description.artistKey }.toMutableList()
            items.add(0, mShuffleItem)
            sortedSongs.postValue(items)
          }
          sortedSongs
        }
        SortingMode.SORT_BY_TITLE -> mAllSongs.switchMap { songs ->
          val sortedSongs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            val items = songs.sortedBy { it.description.titleKey }.toMutableList()
            items.add(0, mShuffleItem)
            sortedSongs.postValue(items)
          }
          sortedSongs
        }
        SortingMode.SORT_BY_DATE -> mAllSongs.switchMap { songs ->
          val sortedSongs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
          AsyncTask.execute {
            val items = songs.sortedByDescending { it.description.dateAdded }.toMutableList()
            items.add(0, mShuffleItem)
            sortedSongs.postValue(items)
          }
          sortedSongs
        }
        else -> throw IllegalArgumentException("unknown sorting constant.")
      }
    }

  val songDescriptions: LiveData<List<MediaDescriptionCompat>>
    get() = songs.switchMap { items ->
      MutableLiveData<List<MediaDescriptionCompat>>().apply {
        AsyncTask.execute {
          postValue(items.filter { it.isPlayable }.map { it.description })
        }
      }
    }

  override fun getLogTag(): String = "SongsViewModel"

}