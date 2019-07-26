package hu.mrolcsi.muzik.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsFragment
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsModule
import hu.mrolcsi.muzik.player.PlayerFragment
import hu.mrolcsi.muzik.player.PlayerModule

@Module
abstract class FragmentsModule {

  @ContributesAndroidInjector(modules = [PlayerModule::class])
  abstract fun contributePlayerFragment(): PlayerFragment

  @ContributesAndroidInjector(modules = [ArtistDetailsModule::class])
  abstract fun contributeArtistDetailsFragment(): ArtistDetailsFragment
}