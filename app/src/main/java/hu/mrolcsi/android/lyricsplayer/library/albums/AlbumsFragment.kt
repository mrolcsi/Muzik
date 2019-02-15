package hu.mrolcsi.android.lyricsplayer.library.albums

import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.library.BrowserFragment
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService
import kotlinx.android.synthetic.main.fragment_browser.*

class AlbumsFragment : BrowserFragment() {

  private val mAlbumsAdapter = AlbumsAdapter()
  private val mLiveAlbums = MediatorLiveData<List<MediaBrowserCompat.MediaItem>>()

  private val mAlbumsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {

      if (arguments != null) {
        val args = AlbumsFragmentArgs.fromBundle(requireArguments())
        if (args.artist != null) {
          AsyncTask.execute {
            val filteredAlbums = children.filter { item ->
              item.description.extras?.getString(MediaStore.Audio.Albums.ARTIST)?.contains(args.artist) ?: false
            }
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