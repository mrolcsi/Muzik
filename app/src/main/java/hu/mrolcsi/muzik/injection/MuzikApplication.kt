package hu.mrolcsi.muzik.injection

import android.app.Application
import hu.mrolcsi.muzik.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class MuzikApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    // Initialize Timber
    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

    startKoin {
      androidLogger()
      androidContext(this@MuzikApplication)
      androidFileProperties()

      // TODO Await fix for Koin and replace the explicit invocations
      //  of loadModules() and createRootScope() with a single call to modules()
      //  (https://github.com/InsertKoinIO/koin/issues/847)
      koin.loadModules(
        listOf(
          appModule,
          dataModule,
          networkModule,
          viewModule
        )
      )
      koin.createRootScope()
    }
  }
}