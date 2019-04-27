package hu.mrolcsi.muzik.library.artists

import android.app.Application
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.library.SessionViewModel
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.extensions.media.artistKey

class ArtistsViewModel(app: Application) : SessionViewModel(app) {

  private val mSubscriptionCallbacks = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      AsyncTask.execute {
        mArtists.postValue(children.sortedBy { it.description.artistKey })
      }
    }
  }

  private val mArtists: MutableLiveData<List<MediaBrowserCompat.MediaItem>> = MutableLiveData()

  init {
    loadArtists()
  }

  private fun loadArtists() {
    mMediaBrowser.subscribe(MuzikBrowserService.MEDIA_ROOT_ARTISTS, mSubscriptionCallbacks)
    mMediaBrowser.connect()
  }

  fun getArtists(): LiveData<List<MediaBrowserCompat.MediaItem>> {
    return mArtists
  }

  override fun getLogTag(): String = "ArtistsViewModel"
}