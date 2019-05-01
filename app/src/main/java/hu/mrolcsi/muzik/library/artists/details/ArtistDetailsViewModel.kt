package hu.mrolcsi.muzik.library.artists.details

import android.app.Application
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hu.mrolcsi.muzik.discogs.DiscogsService
import hu.mrolcsi.muzik.discogs.models.search.SearchResponse
import hu.mrolcsi.muzik.library.SessionViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArtistDetailsViewModel(
  app: Application,
  val artistItem: MediaBrowserCompat.MediaItem
) : SessionViewModel(app) {

  val artistPicture: LiveData<Uri> by lazy {
    MutableLiveData<Uri>().also {
      fetchArtistPicture()
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