package hu.mrolcsi.muzik.library.albums.details

import android.app.Application
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.extensions.switchMap
import hu.mrolcsi.muzik.library.SessionViewModel
import hu.mrolcsi.muzik.service.MuzikBrowserService
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import hu.mrolcsi.muzik.service.extensions.media.trackNumber

class AlbumDetailsViewModel(
  app: Application,
  val albumItem: MediaBrowserCompat.MediaItem
) : SessionViewModel(app) {

  private val mShuffleItem = MediaBrowserCompat.MediaItem(
    MediaDescriptionCompat.Builder()
      .setMediaId("shuffle/all")
      .setTitle(app.getString(R.string.mediaControl_shuffleAll))
      .setIconBitmap(BitmapFactory.decodeResource(app.resources, R.drawable.ic_shuffle))
      .setExtras(bundleOf(MediaType.MEDIA_TYPE_KEY to MediaType.MEDIA_OTHER))
      .build(),
    0
  )

  private val mSubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(
      parentId: String,
      children: MutableList<MediaBrowserCompat.MediaItem>,
      options: Bundle
    ) {
      Log.d(getLogTag(), "Items loaded from MediaBrowser: $children")

      AsyncTask.execute {
        if (children.isNotEmpty()) {
          // Sort by track number and title
          children.sortedBy { it.description.titleKey }.sortedBy { it.description.trackNumber }

          if (children.last().description.trackNumber > 1000) {
            // Add disc number indicators
            val numDiscs = children.last().description.trackNumber / 1000
            if (numDiscs > 0) {
              for (i in 1..numDiscs) {
                val index = children.indexOfFirst { it.description.trackNumber > 1000 }
                val item = MediaBrowserCompat.MediaItem(
                  MediaDescriptionCompat.Builder()
                    .setMediaId("disc/$i")
                    .setTitle(i.toString())
                    .setExtras(bundleOf(MediaType.MEDIA_TYPE_KEY to MediaType.MEDIA_OTHER))
                    .build(),
                  MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
                children.add(index, item)
              }
            }
          }

          // Add Shuffle All item
          children.add(0, mShuffleItem)
        }
        songsFromAlbum.postValue(children)
      }
    }
  }

  val songsFromAlbum: MutableLiveData<List<MediaBrowserCompat.MediaItem>> by lazy {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>>().also {
      loadSongsFromAlbum()
    }
  }

  val songDescriptions: LiveData<List<MediaDescriptionCompat>>
    get() = songsFromAlbum.switchMap { songs ->
      MutableLiveData<List<MediaDescriptionCompat>>().apply {
        AsyncTask.execute {
          postValue(songs.filter { it.isPlayable }.map { it.description })
        }
      }
    }

  private fun loadSongsFromAlbum() {
    mMediaBrowser.subscribe(
      MuzikBrowserService.MEDIA_ROOT_SONGS,
      bundleOf(MuzikBrowserService.OPTION_ALBUM_ID to albumItem.description.id),
      mSubscriptionCallback
    )
  }

  override fun getLogTag() = LOG_TAG

  companion object {
    private const val LOG_TAG = "AlbumDetailsViewModel"
  }

  class Factory(
    private val application: Application,
    private val albumItem: MediaBrowserCompat.MediaItem
  ) : ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return AlbumDetailsViewModel(application, albumItem) as T
    }
  }
}