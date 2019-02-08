package hu.mrolcsi.android.lyricsplayer.library

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService

abstract class BrowserFragment : Fragment() {

  private lateinit var mMediaBrowser: MediaBrowserCompat

  //region CALLBACKS

  private val mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    // TODO
  }

  //endregion

  //region LIFECYCLE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Create MediaBrowserServiceCompat
    context?.let {
      mMediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(it, LPBrowserService::class.java),
        mConnectionCallbacks,
        null // optional Bundle
      )
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_browser, container, false)
  }

  override fun onStart() {
    super.onStart()

    mMediaBrowser.subscribe(getParentId(), getSubscriptionCallback())

    mMediaBrowser.connect()
  }

  override fun onStop() {
    super.onStop()

    mMediaBrowser.unsubscribe(getParentId())

    mMediaBrowser.disconnect()
  }

  //endregion

  abstract fun getParentId(): String

  abstract fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback

}
