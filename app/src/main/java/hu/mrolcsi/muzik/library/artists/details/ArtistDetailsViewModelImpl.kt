package hu.mrolcsi.muzik.library.artists.details

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.discogs.DiscogsService
import hu.mrolcsi.muzik.discogs.models.search.SearchResponse
import hu.mrolcsi.muzik.extensions.switchMap
import hu.mrolcsi.muzik.library.SessionViewModelBase
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ArtistDetailsViewModelImpl @Inject constructor(
  app: Application,
  private val discogs: DiscogsService
) : SessionViewModelBase(app), ArtistDetailsViewModel {

  override var artistItem: MediaBrowserCompat.MediaItem? = null

  private val mShuffleItem = MediaBrowserCompat.MediaItem(
    MediaDescriptionCompat.Builder()
      .setMediaId("shuffle/all")
      .setTitle(app.getString(R.string.mediaControl_shuffleAll))
      .setIconBitmap(BitmapFactory.decodeResource(app.resources, R.drawable.ic_shuffle))
      .setExtras(bundleOf(MediaType.MEDIA_TYPE_KEY to MediaType.MEDIA_OTHER))
      .build(),
    0
  )

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
        val songs = children.sortedBy { it.description.titleKey }.toMutableList()
        songs.add(0, mShuffleItem)
        artistSongs.postValue(songs)
      }
    }
  }

  override val artistPicture: LiveData<Uri> by lazy {
    MutableLiveData<Uri>().also {
      fetchArtistPicture()
    }
  }

  override val artistAlbums: LiveData<List<MediaBrowserCompat.MediaItem>> by lazy {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>>().apply {
      loadAlbumsByArtist()
    }
  }

  override val artistSongs by lazy {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>>().apply {
      loadSongsByArtist()
    }
  }

  override val songDescriptions = artistSongs.switchMap { songs ->
    MutableLiveData<List<MediaDescriptionCompat>>().apply {
      AsyncTask.execute {
        postValue(songs.filter { it.isPlayable }.map { it.description })
      }
    }
  }

  private fun fetchArtistPicture() {
    artistItem?.description?.title?.let { artist ->
      discogs.searchForArtist(artist = artist.toString()).enqueue(object : Callback<SearchResponse> {
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
    mediaBrowser.subscribe(
      MuzikBrowserService.MEDIA_ROOT_ALBUMS,
      bundleOf(MuzikBrowserService.OPTION_ARTIST_ID to artistItem?.description?.id),
      albumsCallback
    )
  }

  private fun loadSongsByArtist() {
    mediaBrowser.subscribe(
      MuzikBrowserService.MEDIA_ROOT_SONGS,
      bundleOf(MuzikBrowserService.OPTION_ARTIST_ID to artistItem?.description?.id),
      songsCallback
    )
  }

  override fun getLogTag(): String = LOG_TAG

  companion object {
    private const val LOG_TAG = "ArtistDetailsViewModel"
  }
}