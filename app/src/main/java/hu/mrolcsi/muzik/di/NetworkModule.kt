package hu.mrolcsi.muzik.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.discogs.DiscogsApi
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

  @Provides
  @Singleton
  fun provideGson(): Gson = GsonBuilder().create()

  @Provides
  @Singleton
  fun provideRetrofit(gson: Gson): Retrofit.Builder =
    Retrofit.Builder()
      .addConverterFactory(GsonConverterFactory.create(gson))
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())

  @Provides
  @Singleton
  fun provideDiscogsService(builder: Retrofit.Builder): DiscogsApi =
    builder
      .baseUrl("https://api.discogs.com/")
      .build()
      .create(DiscogsApi::class.java)
}