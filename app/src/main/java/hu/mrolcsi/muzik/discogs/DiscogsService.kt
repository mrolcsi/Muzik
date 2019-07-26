package hu.mrolcsi.muzik.discogs

import hu.mrolcsi.muzik.BuildConfig
import hu.mrolcsi.muzik.discogs.models.search.SearchResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface DiscogsService {

  @Headers(
    "User-Agent: ${BuildConfig.APPLICATION_ID}",
    "Authorization: Discogs key=$CONSUMER_KEY, secret=$CONSUMER_SECRET"
  )
  @GET("database/search?type=artist&per_page=1")
  fun searchForArtist(@Query("q") artist: String): Call<SearchResponse>

  companion object {
    private const val SERVICE_ROOT = "https://api.discogs.com/"

    private const val CONSUMER_KEY = "jGFYeOxyqgsXUgNLIaQy"
    private const val CONSUMER_SECRET = "oRzoZbmGxqYQHrOFwfAOqrkKUQVKnZWs"

    @Volatile private var instance: DiscogsService? = null

    fun getInstance(baseUrl: String = SERVICE_ROOT): DiscogsService {

      return instance ?: synchronized(this) {
        instance ?: Retrofit.Builder()
          .baseUrl(baseUrl)
          .addConverterFactory(GsonConverterFactory.create())
          .build()
          .create(DiscogsService::class.java).also {
            instance = it
          }
      }
    }
  }

}