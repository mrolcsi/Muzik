package hu.mrolcsi.muzik.discogs

import hu.mrolcsi.muzik.BuildConfig
import hu.mrolcsi.muzik.discogs.models.search.SearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

private const val CONSUMER_KEY = "jGFYeOxyqgsXUgNLIaQy"
private const val CONSUMER_SECRET = "oRzoZbmGxqYQHrOFwfAOqrkKUQVKnZWs"

interface DiscogsService {

  @GET("database/search?type=artist&per_page=1")
  fun searchForArtist(
    @Header("User-Agent") userAgent: String = BuildConfig.APPLICATION_ID,
    @Header("Authorization") authorization: String = "Discogs key=$CONSUMER_KEY, secret=$CONSUMER_SECRET",
    @Query("q") artist: String
  ): Call<SearchResponse>

}