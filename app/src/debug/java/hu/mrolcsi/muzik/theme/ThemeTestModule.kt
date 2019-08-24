package hu.mrolcsi.muzik.theme

import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class ThemeTestModule {

  @Suppress("UNCHECKED_CAST")
  @Provides
  fun providePlayerViewModel(
    activity: ThemeTestActivity,
    provider: Provider<ThemeTestViewModelImpl>
  ): ThemeTestViewModel =
    createOrReUseViewModel(activity, provider)

}