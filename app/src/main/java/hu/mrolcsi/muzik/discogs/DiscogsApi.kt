package hu.mrolcsi.muzik.discogs

import hu.mrolcsi.muzik.BuildConfig
import hu.mrolcsi.muzik.discogs.models.search.SearchResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

private const val CONSUMER_KEY = "jGFYeOxyqgsXUgNLIaQy"
private const val CONSUMER_SECRET = "oRzoZbmGxqYQHrOFwfAOqrkKUQVKnZWs"

interface DiscogsApi {

  @GET("database/search?type=artist&per_page=1&key=$CONSUMER_KEY&secret=$CONSUMER_SECRET")
  fun searchForArtist(
    @Header("User-Agent") userAgent: String = BuildConfig.APPLICATION_ID,
    //@Header("Authorization") authorization: String = "DiscogsApi key=$CONSUMER_KEY, secret=$CONSUMER_SECRET",
    @Query("q") artist: String
  ): Single<SearchResponse>

}