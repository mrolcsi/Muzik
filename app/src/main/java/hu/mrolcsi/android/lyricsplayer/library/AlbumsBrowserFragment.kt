package hu.mrolcsi.android.lyricsplayer.library

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService

class AlbumsBrowserFragment : BrowserFragment() {

  private val mAlbumsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    // TODO
  }

  override fun getParentId(): String {
    return LPBrowserService.MEDIA_ALBUMS_ID
  }

  override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
    return mAlbumsSubscription
  }
}