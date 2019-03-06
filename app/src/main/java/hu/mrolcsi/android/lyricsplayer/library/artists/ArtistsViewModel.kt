package hu.mrolcsi.android.lyricsplayer.library.artists

import android.app.Application
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.android.lyricsplayer.library.LibraryViewModel
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService

class ArtistsViewModel(app: Application) : LibraryViewModel(app) {

  private val mSubscriptionCallbacks = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      mArtists.postValue(children)
    }
  }

  private val mArtists: MutableLiveData<List<MediaBrowserCompat.MediaItem>> = MutableLiveData()

  init {
    loadArtists()
  }

  private fun loadArtists() {
    mMediaBrowser.subscribe(LPBrowserService.MEDIA_ARTISTS_ID, mSubscriptionCallbacks)
    mMediaBrowser.connect()
  }

  fun getArtists(): LiveData<List<MediaBrowserCompat.MediaItem>> {
    return mArtists
  }

  override fun getLogTag(): String = "ArtistsViewModel"
}