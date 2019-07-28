package hu.mrolcsi.muzik.library.artists

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData

interface ArtistsViewModel {

  val artists: LiveData<List<MediaBrowserCompat.MediaItem>>

}