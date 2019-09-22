package hu.mrolcsi.muzik.splash

import com.tbruyelle.rxpermissions2.RxPermissions
import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.di.createOrReUseViewModel
import javax.inject.Provider

@Module
class SplashModule {

  @Provides
  fun provideLibraryViewModel(
    fragment: SplashFragment,
    provider: Provider<SplashViewModelImpl>
  ): SplashViewModel = createOrReUseViewModel(fragment, provider)

  @Provides
  fun provideRxPermissions(
    fragment: SplashFragment
  ): RxPermissions = RxPermissions(fragment)
}