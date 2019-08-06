package hu.mrolcsi.muzik.library.miniplayer

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class MiniPlayerModule {

  @Provides
  fun provideMiniPlayerViewModel(
    fragment: MiniPlayerFragment,
    provider: Provider<MiniPlayerViewModelImpl>
  ): MiniPlayerViewModel = createOrReUseViewModel(fragment, provider)

}