package hu.mrolcsi.muzik.library.artists.details

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class ArtistDetailsModule {

  @Provides
  fun provideArtistDetailsViewModel(
    fragment: ArtistDetailsFragment,
    provider: Provider<ArtistDetailsViewModelImpl>
  ): ArtistDetailsViewModel = createOrReUseViewModel(fragment, provider)

}