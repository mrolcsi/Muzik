package hu.mrolcsi.muzik.library.artists.details

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.library.SessionViewModel

interface ArtistDetailsViewModel : SessionViewModel {

  var artistItem: MediaBrowserCompat.MediaItem?

  val artistSongs: LiveData<List<MediaBrowserCompat.MediaItem>>

  val artistAlbums: LiveData<List<MediaBrowserCompat.MediaItem>>

  val artistPicture: LiveData<Uri>

  val songDescriptions: LiveData<List<MediaDescriptionCompat>>
}