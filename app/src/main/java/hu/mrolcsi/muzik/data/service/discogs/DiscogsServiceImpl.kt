package hu.mrolcsi.muzik.data.service.discogs

import android.net.Uri
import hu.mrolcsi.muzik.data.remote.discogs.DiscogsApi
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class DiscogsServiceImpl : DiscogsService, KoinComponent {

  private val discogsApi: DiscogsApi by inject()

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
        .doOnError { Timber.e("fetchPictureUrl($artist) Got error: $it") }
        .filter { response -> response.results.isNotEmpty() }
        .map { response -> response.results.first() }
        .map { result -> Uri.parse(result.coverImage) }
        .doOnSuccess { cache[artist] = it }
    } ?: Maybe.empty()
  }
}