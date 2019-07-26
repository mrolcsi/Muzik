package hu.mrolcsi.muzik.di

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Component(
  modules = [
    AndroidSupportInjectionModule::class,
    AppModule::class,
    ActivitiesModule::class,
    FragmentsModule::class,
    NetworkModule::class
  ]
)
@Singleton
interface ApplicationComponent : AndroidInjector<MuzikApplication> {

  interface Factory : AndroidInjector.Factory<MuzikApplication>
}