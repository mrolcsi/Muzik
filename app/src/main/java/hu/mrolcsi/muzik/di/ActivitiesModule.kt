package hu.mrolcsi.muzik.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import hu.mrolcsi.muzik.MainActivity
import hu.mrolcsi.muzik.theme.ThemeTestActivity
import hu.mrolcsi.muzik.theme.ThemeTestModule

@Module
abstract class ActivitiesModule {

  @ContributesAndroidInjector   // (modules = [MainActivityModule::class])
  abstract fun contributeMainActivity(): MainActivity

  @ContributesAndroidInjector(modules = [ThemeTestModule::class])
  abstract fun contributeThemeTestActivity(): ThemeTestActivity
}