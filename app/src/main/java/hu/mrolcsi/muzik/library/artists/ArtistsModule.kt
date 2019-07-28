package hu.mrolcsi.muzik.library.artists

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class ArtistsModule {

  @Provides
  fun provideArtistsViewModel(fragment: ArtistsFragment, provider: Provider<ArtistsViewModelImpl>): ArtistsViewModel =
    createOrReUseViewModel(fragment, provider)


}