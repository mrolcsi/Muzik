package hu.mrolcsi.muzik.di

import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

class MuzikApplication : DaggerApplication() {

  override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
    return DaggerApplicationComponent.builder()
      .appModule(AppModule(this))
      .build()
  }

}