package hu.mrolcsi.muzik.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import hu.mrolcsi.muzik.library.LibraryFragment
import hu.mrolcsi.muzik.library.LibraryModule
import hu.mrolcsi.muzik.library.MiniPlayerFragment
import hu.mrolcsi.muzik.library.artists.ArtistsFragment
import hu.mrolcsi.muzik.library.artists.ArtistsModule
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsFragment
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsModule
import hu.mrolcsi.muzik.player.PlayerFragment
import hu.mrolcsi.muzik.player.PlayerModule
import hu.mrolcsi.muzik.player.playlist.PlaylistFragment
import hu.mrolcsi.muzik.player.playlist.PlaylistModule

@Suppress("unused")
@Module
abstract class FragmentsModule {

  @ContributesAndroidInjector(modules = [LibraryModule::class])
  abstract fun contributeLibraryFragment(): LibraryFragment

  @ContributesAndroidInjector(modules = [LibraryModule::class])
  abstract fun contributeMiniPlayerFragment(): MiniPlayerFragment

  @ContributesAndroidInjector(modules = [ArtistsModule::class])
  abstract fun contributeArtistsFragment(): ArtistsFragment

  @ContributesAndroidInjector(modules = [ArtistDetailsModule::class])
  abstract fun contributeArtistDetailsFragment(): ArtistDetailsFragment

  @ContributesAndroidInjector(modules = [PlayerModule::class])
  abstract fun contributePlayerFragment(): PlayerFragment

  @ContributesAndroidInjector(modules = [PlaylistModule::class])
  abstract fun contributePlaylistFragment(): PlaylistFragment
}