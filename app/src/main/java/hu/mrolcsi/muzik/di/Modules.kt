package hu.mrolcsi.muzik.di

import android.Manifest
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.albums.AlbumsViewModelImpl
import hu.mrolcsi.muzik.albums.details.AlbumDetailsViewModelImpl
import hu.mrolcsi.muzik.artists.ArtistsViewModelImpl
import hu.mrolcsi.muzik.artists.details.ArtistDetailsViewModelImpl
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
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaRepositoryImpl
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.media.MediaServiceImpl
import hu.mrolcsi.muzik.miniplayer.MiniPlayerViewModelImpl
import hu.mrolcsi.muzik.player.PlayerViewModelImpl
import hu.mrolcsi.muzik.player.playlist.PlaylistViewModelImpl
import hu.mrolcsi.muzik.songs.SongsViewModelImpl
import hu.mrolcsi.muzik.theme.ThemeService
import hu.mrolcsi.muzik.theme.ThemeServiceImpl
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
  single<MediaService> { MediaServiceImpl() }
  single<DiscogsService> { DiscogsServiceImpl() }
  single<ThemeService> { ThemeServiceImpl() }
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

  single<MediaRepository> { MediaRepositoryImpl() }
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