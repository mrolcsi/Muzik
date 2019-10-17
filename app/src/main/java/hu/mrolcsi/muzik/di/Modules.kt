package hu.mrolcsi.muzik.di

import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.common.AnnotationExclusionStrategy
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.muzik.database.playqueue.PlayQueueMigrations
import hu.mrolcsi.muzik.discogs.DiscogsApi
import hu.mrolcsi.muzik.discogs.DiscogsService
import hu.mrolcsi.muzik.discogs.DiscogsServiceImpl
import hu.mrolcsi.muzik.library.LibraryViewModelImpl
import hu.mrolcsi.muzik.library.albums.AlbumsViewModelImpl
import hu.mrolcsi.muzik.library.albums.details.AlbumDetailsViewModelImpl
import hu.mrolcsi.muzik.library.artists.ArtistsViewModelImpl
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsViewModelImpl
import hu.mrolcsi.muzik.library.miniplayer.MiniPlayerViewModelImpl
import hu.mrolcsi.muzik.library.pager.LibraryPagerViewModelImpl
import hu.mrolcsi.muzik.library.songs.SongsViewModelImpl
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaRepositoryImpl
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.media.MediaServiceImpl
import hu.mrolcsi.muzik.player.PlayerViewModelImpl
import hu.mrolcsi.muzik.player.playlist.PlaylistViewModelImpl
import hu.mrolcsi.muzik.splash.SplashViewModelImpl
import hu.mrolcsi.muzik.theme.ThemeService
import hu.mrolcsi.muzik.theme.ThemeServiceImpl
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
  single<MediaService> { MediaServiceImpl(get()) }
  single<DiscogsService> { DiscogsServiceImpl(get()) }
  single<ThemeService> { ThemeServiceImpl(get(), get()) }
  single<SharedPreferences> { PreferenceManager.getDefaultSharedPreferences(get()) }
}

val dataModule = module {

  single {
    Room.databaseBuilder(get(), PlayQueueDatabase::class.java, PlayQueueDatabase.DATABASE_NAME)
      .addMigrations(
        PlayQueueMigrations.MIGRATION_1_2,
        PlayQueueMigrations.MIGRATION_2_3
      ).build()
  }

  single { get<PlayQueueDatabase>().getPlayQueueDao() }

  single<MediaRepository> { MediaRepositoryImpl(get()) }
}

val networkModule = module {

  single<Gson> {
    GsonBuilder()
      .setExclusionStrategies(AnnotationExclusionStrategy())
      .create()
  }

  single<Retrofit.Builder> {
    Retrofit.Builder()
      .addConverterFactory(GsonConverterFactory.create(get()))
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
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
  factory { ThemedViewModelImpl(get()) }
  factory { (fragment: Fragment) -> RxPermissions(fragment) }

  viewModel { (rxPermissions: RxPermissions) -> SplashViewModelImpl(get(), get(), get(), get(), rxPermissions) }
  viewModel { LibraryViewModelImpl(get(), get()) }
  viewModel { LibraryPagerViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
  viewModel { ArtistsViewModelImpl(get(), get(), get(), get(), get()) }
  viewModel { ArtistDetailsViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
  viewModel { AlbumsViewModelImpl(get(), get(), get(), get(), get()) }
  viewModel { AlbumDetailsViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
  viewModel { SongsViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
  viewModel { MiniPlayerViewModelImpl(get(), get(), get(), get(), get(), get()) }
  viewModel { PlayerViewModelImpl(get(), get(), get(), get(), get(), get()) }
  viewModel { PlaylistViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
}