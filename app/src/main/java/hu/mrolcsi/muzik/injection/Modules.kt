package hu.mrolcsi.muzik.injection

import android.Manifest
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.data.local.MuzikDatabase
import hu.mrolcsi.muzik.data.local.MuzikDatabaseMigrations
import hu.mrolcsi.muzik.data.manager.media.MediaManager
import hu.mrolcsi.muzik.data.manager.media.MediaManagerImpl
import hu.mrolcsi.muzik.data.remote.AnnotationExclusionStrategy
import hu.mrolcsi.muzik.data.remote.discogs.DiscogsApi
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.data.repository.media.MediaRepositoryImpl
import hu.mrolcsi.muzik.data.service.discogs.DiscogsService
import hu.mrolcsi.muzik.data.service.discogs.DiscogsServiceImpl
import hu.mrolcsi.muzik.data.service.theme.ThemeService
import hu.mrolcsi.muzik.data.service.theme.ThemeServiceImpl
import hu.mrolcsi.muzik.ui.albumDetails.AlbumDetailsViewModelImpl
import hu.mrolcsi.muzik.ui.albums.AlbumsViewModelImpl
import hu.mrolcsi.muzik.ui.artistDetails.ArtistDetailsViewModelImpl
import hu.mrolcsi.muzik.ui.artists.ArtistsViewModelImpl
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.library.LibraryViewModelImpl
import hu.mrolcsi.muzik.ui.miniPlayer.MiniPlayerViewModelImpl
import hu.mrolcsi.muzik.ui.player.PlayerViewModelImpl
import hu.mrolcsi.muzik.ui.playlist.PlaylistViewModelImpl
import hu.mrolcsi.muzik.ui.songs.SongsViewModelImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
  single<MediaManager> { MediaManagerImpl() }
  single<DiscogsService> { DiscogsServiceImpl() }
  single<ThemeService> { ThemeServiceImpl() }
  single<SharedPreferences> { PreferenceManager.getDefaultSharedPreferences(get()) }
}

val dataModule = module {

  single {
    Room.databaseBuilder(get(), MuzikDatabase::class.java, MuzikDatabase.DATABASE_NAME)
      .addMigrations(
        MuzikDatabaseMigrations.MIGRATION_1_2,
        MuzikDatabaseMigrations.MIGRATION_2_3
      ).build()
  }

  single { get<MuzikDatabase>().getPlayQueueDao() }

  single<MediaRepository> { MediaRepositoryImpl() }
}

val networkModule = module {

  single<Gson> {
    GsonBuilder()
      .setExclusionStrategies(AnnotationExclusionStrategy())
      .create()
  }

  single {
    val loggingInterceptor = HttpLoggingInterceptor(
      object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
          Log.v("HTTP", message)
        }
      }).apply {
      level = HttpLoggingInterceptor.Level.BODY
    }

    OkHttpClient.Builder()
      .addInterceptor(loggingInterceptor)
      .build()
  }

  single<Retrofit.Builder> {
    Retrofit.Builder()
      .addConverterFactory(GsonConverterFactory.create(get()))
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .client(get<OkHttpClient>())
  }

  single<DiscogsApi> {
    get<Retrofit.Builder>()
      .baseUrl("https://api.discogs.com/")
      .build()
      .create(DiscogsApi::class.java)
  }
}

val viewModule = module {
  factory { ObservableImpl() }
  factory { ExecuteOnceUiCommandSource() }
  factory { ExecuteOnceNavCommandSource() }
  factory { ThemedViewModelImpl() }
  factory { (fragment: Fragment) -> RxPermissions(fragment) }

  single(qualifier = named(REQUIRED_PERMISSIONS)) {
    arrayOf(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
  }

  viewModel { (fragment: Fragment) ->
    LibraryViewModelImpl(get(), get(), get(), get(), get { parametersOf(fragment) })
  }
  viewModel { ArtistsViewModelImpl(get(), get(), get(), get()) }
  viewModel { ArtistDetailsViewModelImpl(get(), get(), get(), get()) }
  viewModel { AlbumsViewModelImpl(get(), get(), get(), get()) }
  viewModel { AlbumDetailsViewModelImpl(get(), get(), get(), get()) }
  viewModel { SongsViewModelImpl(get(), get(), get(), get()) }
  viewModel { MiniPlayerViewModelImpl(get(), get(), get(), get()) }
  viewModel { PlayerViewModelImpl(get(), get(), get(), get()) }
  viewModel { PlaylistViewModelImpl(get(), get(), get(), get(), get()) }
}

const val REQUIRED_PERMISSIONS = "REQUIRED_PERMISSIONS"