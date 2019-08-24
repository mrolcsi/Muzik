package hu.mrolcsi.muzik.library

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import hu.mrolcsi.muzik.library.pager.LibraryPagerFragment
import hu.mrolcsi.muzik.library.pager.LibraryPagerViewModel
import hu.mrolcsi.muzik.library.pager.LibraryPagerViewModelImpl
import javax.inject.Provider

@Module
class LibraryModule {

  @Provides
  fun provideLibraryViewModel(
    fragment: LibraryFragment,
    provider: Provider<LibraryViewModelImpl>
  ): LibraryViewModel = createOrReUseViewModel(fragment, provider)

  @Provides
  fun provideLibraryPagerViewModel(
    fragment: LibraryPagerFragment,
    provider: Provider<LibraryPagerViewModelImpl>
  ): LibraryPagerViewModel = createOrReUseViewModel(fragment, provider)

}