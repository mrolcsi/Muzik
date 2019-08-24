package hu.mrolcsi.muzik.theme

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.common.viewmodel.RxViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

interface ThemedViewModel {
  val previousTheme: Theme?
  val currentTheme: LiveData<Theme>
}

open class ThemedViewModelImpl @Inject constructor(
  val themeService: ThemeService
) : RxViewModel(), ThemedViewModel {

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