package hu.mrolcsi.android.lyricsplayer.library

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService

class SongsBrowserFragment : BrowserFragment() {

  private val mSongsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    // TODO
  }

  override fun getParentId(): String {
    return LPBrowserService.MEDIA_SONGS_ID
  }

  override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
    return mSongsSubscription
  }
}