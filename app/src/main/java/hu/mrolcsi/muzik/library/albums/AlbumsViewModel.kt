package hu.mrolcsi.muzik.library.albums

import android.app.Application
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.library.SessionViewModel
import hu.mrolcsi.muzik.service.MuzikBrowserService

class AlbumsViewModel(app: Application) : SessionViewModel(app) {

  // TODO: do filtering through Transformation.map?

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

  val albums: LiveData<List<MediaBrowserCompat.MediaItem>>
    get() = mAllAlbums

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

  override fun getLogTag(): String = "AlbumsViewModel"

  data class ArtistInfo(
    val artistKey: String?,
    val artistName: String?,
    val numberOfTracks: Int
  )
}