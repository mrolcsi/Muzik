package hu.mrolcsi.muzik.library.artists.details

import android.app.Application
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hu.mrolcsi.muzik.discogs.DiscogsService
import hu.mrolcsi.muzik.discogs.models.search.SearchResponse
import hu.mrolcsi.muzik.library.SessionViewModel
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArtistDetailsViewModel(
  app: Application,
  val artistItem: MediaBrowserCompat.MediaItem
) : SessionViewModel(app) {

  private val albumsCallback = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(
      parentId: String,
      children: MutableList<MediaBrowserCompat.MediaItem>,
      options: Bundle
    ) {
      Log.d(getLogTag(), "Albums loaded from MediaBrowser: $children")

      AsyncTask.execute {
        (artistAlbums as MutableLiveData).postValue(
          children.sortedBy { it.description.titleKey }
        )
      }
    }
  }

  private val songsCallback = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(
      parentId: String,
      children: MutableList<MediaBrowserCompat.MediaItem>,
      options: Bundle
    ) {
      Log.d(getLogTag(), "Songs loaded from MediaBrowser: $children")

      AsyncTask.execute {
        (artistSongs as MutableLiveData).postValue(
          children.sortedBy { it.description.titleKey }
        )
      }
    }
  }

  val artistPicture: LiveData<Uri> by lazy {
    MutableLiveData<Uri>().also {
      fetchArtistPicture()
    }
  }

  val artistAlbums: LiveData<List<MediaBrowserCompat.MediaItem>> by lazy {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>>().apply {
      loadAlbumsByArtist()
    }
  }

  val artistSongs: LiveData<List<MediaBrowserCompat.MediaItem>> by lazy {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>>().apply {
      loadSongsByArtist()
    }
  }

  private fun fetchArtistPicture() {
    artistItem.description.title?.let { artist ->
      DiscogsService.getInstance().searchForArtist(artist.toString()).enqueue(object : Callback<SearchResponse> {
        override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
          Log.e(LOG_TAG, t.message)
        }

        override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
          response.body()?.let { body ->
            body.results.takeIf { it.isNotEmpty() }?.first()?.let { result ->
              (artistPicture as MutableLiveData).postValue(Uri.parse(result.coverImage))
            }
          }
        }

      })
    }
  }

  private fun loadAlbumsByArtist() {
    mMediaBrowser.subscribe(
      MuzikBrowserService.MEDIA_ROOT_ALBUMS,
      bundleOf(MuzikBrowserService.OPTION_ARTIST_ID to artistItem.description.id),
      albumsCallback
    )
  }

  private fun loadSongsByArtist() {
    mMediaBrowser.subscribe(
      MuzikBrowserService.MEDIA_ROOT_SONGS,
      bundleOf(MuzikBrowserService.OPTION_ARTIST_ID to artistItem.description.id),
      songsCallback
    )
  }

  override fun getLogTag(): String = LOG_TAG

  class Factory(
    private val application: Application,
    private val artistItem: MediaBrowserCompat.MediaItem
  ) : ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return ArtistDetailsViewModel(application, artistItem) as T
    }
  }

  companion object {
    private const val LOG_TAG = "ArtistDetailsViewModel"
  }
}