package hu.mrolcsi.muzik.library.albums

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class AlbumsModule {

  @Provides
  fun provideAlbumsViewModel(fragment: AlbumsFragment, provider: Provider<AlbumsViewModelImpl>): AlbumsViewModel =
    createOrReUseViewModel(fragment, provider)

}