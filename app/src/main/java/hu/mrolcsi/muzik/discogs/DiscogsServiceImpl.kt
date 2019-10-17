package hu.mrolcsi.muzik.discogs

import android.net.Uri
import com.google.android.exoplayer2.util.Log
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers

class DiscogsServiceImpl constructor(
  private val discogsApi: DiscogsApi
) : DiscogsService {

  // TODO: Disk cache?

  private val cache = HashMap<String, Uri>()

  override fun getArtistPictureUrl(artistName: String?): Maybe<Uri> {
    val pictureUri = cache[artistName]
    return if (pictureUri != null) {
      Maybe.just(pictureUri)
    } else {
      fetchPictureUrl(artistName)
    }
  }

  private fun fetchPictureUrl(artistName: String?): Maybe<Uri> {
    return artistName?.let { artist ->
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