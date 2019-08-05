package hu.mrolcsi.muzik.library.albums.details

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class AlbumDetailsModule {

  @Provides
  fun provideAlbumDetailsViewModel(
    fragment: AlbumDetailsFragment,
    provider: Provider<AlbumDetailsViewModelImpl>
  ): AlbumDetailsViewModel =
    createOrReUseViewModel(fragment, provider)

}