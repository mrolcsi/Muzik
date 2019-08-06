package hu.mrolcsi.muzik.library

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import hu.mrolcsi.muzik.library.artists.ArtistsFragment
import hu.mrolcsi.muzik.library.artists.ArtistsViewModel
import hu.mrolcsi.muzik.library.artists.ArtistsViewModelImpl
import hu.mrolcsi.muzik.library.miniplayer.MiniPlayerFragment
import hu.mrolcsi.muzik.player.PlayerViewModel
import hu.mrolcsi.muzik.player.PlayerViewModelImpl
import javax.inject.Provider

@Module
class LibraryModule {

  @Provides
  fun providePlayerViewModel(fragment: MiniPlayerFragment, provider: Provider<PlayerViewModelImpl>): PlayerViewModel =
    createOrReUseViewModel(fragment, provider)

  @Provides
  fun provideArtistsViewModel(fragment: ArtistsFragment, provider: Provider<ArtistsViewModelImpl>): ArtistsViewModel =
    createOrReUseViewModel(fragment, provider)


}