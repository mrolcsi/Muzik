package hu.mrolcsi.android.lyricsplayer.library.albums

import android.app.Application
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.switchMap
import hu.mrolcsi.android.lyricsplayer.library.LibraryViewModel
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService

class AlbumsViewModel(app: Application) : LibraryViewModel(app) {

  // TODO: do filtering through Transformation.map?

  private val mSubscriptionCallbacks = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      Log.d(LOG_TAG, "Items loaded from MediaBrowser: $children")
      mAllAlbums.postValue(children)
    }
  }

  private val mAllAlbums: MutableLiveData<List<MediaBrowserCompat.MediaItem>> by lazy {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>>().also {
      loadAlbums()
    }
  }

  private fun loadAlbums() {
    mMediaBrowser.subscribe(LPBrowserService.MEDIA_ALBUMS_ID, mSubscriptionCallbacks)
    mMediaBrowser.connect()
  }

  fun getAlbums(): LiveData<List<MediaBrowserCompat.MediaItem>> {
    // When "songFilter" changes, replace the contents of "albums" with the contents of "result".
    return artistFilter.switchMap { artist ->
      when (artist) {
        null -> mAllAlbums
        else -> {
          mAllAlbums.switchMap { allAlbums ->
            val filteredAlbums = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
            AsyncTask.execute {
              filteredAlbums.postValue(filterByArtist(allAlbums, artist))
            }
            filteredAlbums
          }
        }
      }
    }
  }

  val artistFilter = MutableLiveData<ArtistInfo>()

  private fun filterByArtist(
    allAlbums: List<MediaBrowserCompat.MediaItem>,
    info: ArtistInfo
  ): MutableList<MediaBrowserCompat.MediaItem> {
    val albumsByArtist = allAlbums.filter {
      it.description.extras?.getString(MediaStore.Audio.Albums.ARTIST) == info.artistName
    }.toMutableList()
    // Add "All songs" as first item
    val allSongsItem = MediaBrowserCompat.MediaItem(
      MediaDescriptionCompat.Builder()
        .setMediaId(AlbumsAdapter.MEDIA_ID_ALL_SONGS)
        .setTitle(getApplication<Application>().getString(R.string.albums_showAllSongs))
        .setSubtitle(
          getApplication<Application>().resources.getQuantityString(
            R.plurals.artists_numberOfSongs,
            info.numberOfTracks,
            info.numberOfTracks
          )
        )
        .setExtras(
          bundleOf(
            MediaStore.Audio.ArtistColumns.ARTIST_KEY to info.artistKey,
            MediaStore.Audio.ArtistColumns.ARTIST to info.artistName
          )
        )
        .build(),
      MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
    )
    albumsByArtist.add(0, allSongsItem)
    return albumsByArtist
  }

  data class ArtistInfo(
    val artistKey: String?,
    val artistName: String?,
    val numberOfTracks: Int
  )

  companion object {
    private const val LOG_TAG = "AlbumsViewModel"
  }
}