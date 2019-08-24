package hu.mrolcsi.muzik.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import hu.mrolcsi.muzik.service.MuzikPlayerService
import hu.mrolcsi.muzik.service.MuzikPlayerServiceModule

@Module
abstract class ServicesModule {

  @ContributesAndroidInjector(modules = [MuzikPlayerServiceModule::class])
  abstract fun contributeMuzikPlayerService(): MuzikPlayerService

}