package hu.mrolcsi.muzik.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import hu.mrolcsi.muzik.library.LibraryFragment
import hu.mrolcsi.muzik.library.LibraryModule
import hu.mrolcsi.muzik.library.albums.AlbumsFragment
import hu.mrolcsi.muzik.library.albums.AlbumsModule
import hu.mrolcsi.muzik.library.albums.details.AlbumDetailsFragment
import hu.mrolcsi.muzik.library.albums.details.AlbumDetailsModule
import hu.mrolcsi.muzik.library.artists.ArtistsFragment
import hu.mrolcsi.muzik.library.artists.ArtistsModule
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsFragment
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsModule
import hu.mrolcsi.muzik.library.miniplayer.MiniPlayerFragment
import hu.mrolcsi.muzik.library.miniplayer.MiniPlayerModule
import hu.mrolcsi.muzik.library.pager.LibraryPagerFragment
import hu.mrolcsi.muzik.library.songs.SongsFragment
import hu.mrolcsi.muzik.library.songs.SongsModule
import hu.mrolcsi.muzik.player.PlayerFragment
import hu.mrolcsi.muzik.player.PlayerModule
import hu.mrolcsi.muzik.player.playlist.PlaylistFragment
import hu.mrolcsi.muzik.player.playlist.PlaylistModule

@Module
abstract class FragmentsModule {

  @ContributesAndroidInjector(modules = [LibraryModule::class])
  abstract fun contributeLibraryFragment(): LibraryFragment

  @ContributesAndroidInjector(modules = [LibraryModule::class])
  abstract fun contributeLibraryPagerFragment(): LibraryPagerFragment

  @ContributesAndroidInjector(modules = [ArtistsModule::class])
  abstract fun contributeArtistsFragment(): ArtistsFragment

  @ContributesAndroidInjector(modules = [ArtistDetailsModule::class])
  abstract fun contributeArtistDetailsFragment(): ArtistDetailsFragment

  @ContributesAndroidInjector(modules = [AlbumsModule::class])
  abstract fun contributeAlbumsFragment(): AlbumsFragment

  @ContributesAndroidInjector(modules = [AlbumDetailsModule::class])
  abstract fun contributeAlbumDetailsFragment(): AlbumDetailsFragment

  @ContributesAndroidInjector(modules = [SongsModule::class])
  abstract fun contributeSongsFragment(): SongsFragment

  @ContributesAndroidInjector(modules = [MiniPlayerModule::class])
  abstract fun contributeMiniPlayerFragment(): MiniPlayerFragment

  @ContributesAndroidInjector(modules = [PlayerModule::class])
  abstract fun contributePlayerFragment(): PlayerFragment

  @ContributesAndroidInjector(modules = [PlaylistModule::class])
  abstract fun contributePlaylistFragment(): PlaylistFragment
}