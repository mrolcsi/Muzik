package hu.mrolcsi.muzik.library.songs

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class SongsModule {

  @Provides
  fun provideSongsViewModel(fragment: SongsFragment, provider: Provider<SongsViewModelImpl>): SongsViewModel =
    createOrReUseViewModel(fragment, provider)

}