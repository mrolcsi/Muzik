package hu.mrolcsi.muzik.di

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.Binds
import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.discogs.DiscogsService
import hu.mrolcsi.muzik.discogs.DiscogsServiceImpl
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaRepositoryImpl
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.media.MediaServiceImpl
import javax.inject.Provider
import javax.inject.Singleton

@Module(includes = [AppModule.DelegateBindings::class])
class AppModule(private val app: Application) {

  @Module
  interface DelegateBindings {

    @Binds
    @Singleton
    fun provideMediaService(delegate: MediaServiceImpl): MediaService

    @Binds
    @Singleton
    fun provideMediaRepository(delegate: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    fun provideDiscogsService(delegate: DiscogsServiceImpl): DiscogsService
  }

  @Provides
  @Singleton
  fun provideApplication(): Application = app

  @Provides
  @Singleton
  fun provideContext(): Context = app.applicationContext

}

@Suppress("UNCHECKED_CAST")
inline fun <Activity : FragmentActivity, reified ViewModelImpl : ViewModel> createOrReUseViewModel(
  activity: Activity,
  provider: Provider<ViewModelImpl>
): ViewModelImpl {
  return ViewModelProviders.of(activity, object : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = provider.get() as T
  }).get(ViewModelImpl::class.java)
}

@Suppress("UNCHECKED_CAST")
inline fun <Frag : Fragment, reified ViewModelImpl : ViewModel> createOrReUseViewModel(
  fragment: Frag,
  provider: Provider<ViewModelImpl>
): ViewModelImpl {
  return ViewModelProviders.of(fragment, object : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = provider.get() as T
  }).get(ViewModelImpl::class.java)
}