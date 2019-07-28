package hu.mrolcsi.muzik.player.playlist

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import hu.mrolcsi.muzik.player.PlayerViewModel
import hu.mrolcsi.muzik.player.PlayerViewModelImpl
import javax.inject.Provider

@Module
class PlaylistModule {

  @Provides
  fun providePlayerViewModelForPlaylist(
    fragment: PlaylistFragment,
    provider: Provider<PlayerViewModelImpl>
  ): PlayerViewModel =
    createOrReUseViewModel(fragment, provider)

}