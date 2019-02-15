package hu.mrolcsi.android.lyricsplayer.library.albums

import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.view.View
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.library.BrowserFragment
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService
import kotlinx.android.synthetic.main.fragment_browser.*

class AlbumsFragment : BrowserFragment() {

  private var mAlbumsAdapter: AlbumsAdapter = AlbumsAdapter()
  private val mLiveAlbums = MediatorLiveData<List<MediaBrowserCompat.MediaItem>>()

  private val mAlbumsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {

      if (arguments != null) {
        val args = AlbumsFragmentArgs.fromBundle(requireArguments())
        if (args.artistName != null) {
          AsyncTask.execute {
            // Filter albums by artist
            val filteredAlbums = children.filter { item ->
              item.description.extras?.getString(MediaStore.Audio.Albums.ARTIST) == args.artistName
            }.toMutableList()
            // Add "All songs" as first item
            val allSongsItem = MediaBrowserCompat.MediaItem(
              MediaDescriptionCompat.Builder()
                .setMediaId(AlbumsAdapter.MEDIA_ID_ALL_SONGS)
                .setTitle(getString(R.string.albums_showAllSongs))
                .setSubtitle("${args.numberOfTracks} songs")
                .setExtras(Bundle().apply {
                  putString(MediaStore.Audio.ArtistColumns.ARTIST_KEY, args.artistKey)
                  putString(MediaStore.Audio.ArtistColumns.ARTIST, args.artistName)
                })
                .build(),
              MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            )
            filteredAlbums.add(0, allSongsItem)
            // Post list to UI
            mLiveAlbums.postValue(filteredAlbums)
          }
        } else {
          mLiveAlbums.postValue(children)
        }
      } else {
        mLiveAlbums.postValue(children)
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mAlbumsAdapter

    mLiveAlbums.observe(this, Observer {
      mAlbumsAdapter.submitList(it)
    })
  }

  override fun getParentId(): String {
    return LPBrowserService.MEDIA_ALBUMS_ID
  }

  override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
    return mAlbumsSubscription
  }
}