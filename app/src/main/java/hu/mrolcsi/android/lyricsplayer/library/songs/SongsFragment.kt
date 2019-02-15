package hu.mrolcsi.android.lyricsplayer.library.songs

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

class SongsFragment : BrowserFragment() {

  private val mSongsAdapter = SongsAdapter()
  private val mLiveSongs = MediatorLiveData<List<MediaBrowserCompat.MediaItem>>()

  private val mSongsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {

      if (arguments != null) {
        val args = SongsFragmentArgs.fromBundle(requireArguments())
        if (args.albumKey != null) {
          AsyncTask.execute {
            val filteredSongs = children.filter { item ->
              item.description.extras?.getString(MediaStore.Audio.Media.ALBUM_KEY) == args.albumKey
            }
            mLiveSongs.postValue(filteredSongs)
          }
        } else {
          mLiveSongs.postValue(children)
        }
      } else {
        mLiveSongs.postValue(children)
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mSongsAdapter

    mLiveSongs.observe(this, Observer {
      mSongsAdapter.submitList(it)
    })
  }

  override fun getParentId(): String {
    return LPBrowserService.MEDIA_SONGS_ID
  }

  override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
    return mSongsSubscription
  }
}