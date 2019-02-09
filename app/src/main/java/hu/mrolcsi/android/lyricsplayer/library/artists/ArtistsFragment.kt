package hu.mrolcsi.android.lyricsplayer.library.artists

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.library.BrowserFragment
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService
import kotlinx.android.synthetic.main.fragment_browser.*

class ArtistsFragment : BrowserFragment() {

  private val mArtistAdapter = ArtistAdapter()

  private val mArtistsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      mArtistAdapter.submitList(children)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mArtistAdapter
  }

  override fun getParentId(): String {
    return LPBrowserService.MEDIA_ARTISTS_ID
  }

  override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
    return mArtistsSubscription
  }
}