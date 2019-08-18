package hu.mrolcsi.muzik.player.playlist

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class PlaylistModule {

  @Provides
  fun providePlayerViewModelForPlaylist(
    fragment: PlaylistFragment,
    provider: Provider<PlaylistViewModelImpl>
  ): PlaylistViewModel =
    createOrReUseViewModel(fragment, provider)

}