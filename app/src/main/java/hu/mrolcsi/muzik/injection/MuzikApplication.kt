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
      // use AndroidLogger as Koin Logger - default Level.INFO
      androidLogger()

      // use the Android context given there
      androidContext(this@MuzikApplication)

      // load properties from assets/koin.properties file
      androidFileProperties()

      // module list
      modules(
        listOf(
          appModule,
          dataModule,
          networkModule,
          viewModule
        )
      )
    }
  }
}