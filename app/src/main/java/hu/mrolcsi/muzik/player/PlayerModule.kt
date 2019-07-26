package hu.mrolcsi.muzik.player

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class PlayerModule {

  @Provides
  fun providePlayerViewModel(fragment: PlayerFragment, provider: Provider<PlayerViewModelImpl>): PlayerViewModel =
    createOrReUseViewModel(fragment, provider)

}