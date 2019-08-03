package hu.mrolcsi.muzik.discogs

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import io.reactivex.Maybe

interface DiscogsService {

  fun getArtistPictureUrl(artistItem: MediaBrowserCompat.MediaItem): Maybe<Uri>

}