package hu.mrolcsi.android.lyricsplayer.library.songs

import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.library.BrowserFragment
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService
import kotlinx.android.synthetic.main.fragment_browser.*

class SongsFragment : BrowserFragment() {

  private val mSongsAdapter = SongsAdapter()

  private val mSongsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {

      var songs = children

      arguments?.let {
        val args = SongsFragmentArgs.fromBundle(it)
        args.argAlbum?.let {
          songs = children.filter { item ->
            item.description.extras?.getString(MediaStore.Audio.Media.ALBUM_KEY) == it
          }.toMutableList()
        }
      }

      mSongsAdapter.submitList(songs)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mSongsAdapter
  }

  override fun getParentId(): String {
    return LPBrowserService.MEDIA_SONGS_ID
  }

  override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
    return mSongsSubscription
  }
}