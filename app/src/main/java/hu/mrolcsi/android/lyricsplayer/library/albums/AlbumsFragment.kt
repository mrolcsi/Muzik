package hu.mrolcsi.android.lyricsplayer.library.albums

import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.library.BrowserFragment
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService
import kotlinx.android.synthetic.main.fragment_browser.*

class AlbumsFragment : BrowserFragment() {

  private val mAlbumsAdapter = AlbumsAdapter()

  private val mAlbumsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {

      var albums = children

      arguments?.let {
        val args = AlbumsFragmentArgs.fromBundle(it)
        args.argArtist?.let {
          albums = children.filter { item ->
            item.description.extras?.getString(MediaStore.Audio.Albums.ARTIST)?.contains(it) ?: false
          }.toMutableList()
        }
      }

      mAlbumsAdapter.submitList(albums)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mAlbumsAdapter
  }

  override fun getParentId(): String {
    return LPBrowserService.MEDIA_ALBUMS_ID
  }

  override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
    return mAlbumsSubscription
  }
}