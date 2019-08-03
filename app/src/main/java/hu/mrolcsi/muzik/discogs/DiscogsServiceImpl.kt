package hu.mrolcsi.muzik.discogs

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import com.google.android.exoplayer2.util.Log
import hu.mrolcsi.muzik.service.extensions.media.artist
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class DiscogsServiceImpl @Inject constructor(
  private val discogsApi: DiscogsApi
) : DiscogsService {

  // TODO: Disk cache?

  private val cache = HashMap<String, Uri>()

  override fun getArtistPictureUrl(artistItem: MediaBrowserCompat.MediaItem): Maybe<Uri> {
    val pictureUri = cache[artistItem.description.artist]
    return if (pictureUri != null) {
      Maybe.just(pictureUri)
    } else {
      fetchPictureUrl(artistItem)
    }
  }

  private fun fetchPictureUrl(artistItem: MediaBrowserCompat.MediaItem): Maybe<Uri> {
    return artistItem.description.artist?.let { artist ->
      discogsApi.searchForArtist(artist = artist)
        .subscribeOn(Schedulers.io())
        .doOnError { Log.e("DiscogsService", "fetchPictureUrl($artist) Got error: $it") }
        .filter { response -> response.results.isNotEmpty() }
        .map { response -> response.results.first() }
        .map { result -> Uri.parse(result.coverImage) }
        .doOnSuccess { cache[artist] = it }
    } ?: Maybe.empty()
  }
}