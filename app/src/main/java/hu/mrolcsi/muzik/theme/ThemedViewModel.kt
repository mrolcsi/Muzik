package hu.mrolcsi.muzik.theme

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.common.viewmodel.RxViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import org.koin.core.inject

interface ThemedViewModel {

  val themeService: ThemeService

  val previousTheme: Theme?
  val currentTheme: LiveData<Theme>
}

open class ThemedViewModelImpl : RxViewModel(), ThemedViewModel, KoinComponent {

  final override val themeService: ThemeService by inject()

  override var previousTheme: Theme? = null
  override val currentTheme = MutableLiveData<Theme>()

  init {
    themeService.currentTheme
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext { Log.d("ThemedViewModel", "Theme updated: $it") }
      .scan { old: Theme, new: Theme ->
        previousTheme = old
        new
      }
      .subscribeBy(
        onNext = { currentTheme.value = it },
        onError = { Log.e("ThemedViewModel", Log.getStackTraceString(it)) }
      ).disposeOnCleared()
  }

}