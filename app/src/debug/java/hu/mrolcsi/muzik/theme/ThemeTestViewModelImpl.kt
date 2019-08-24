package hu.mrolcsi.muzik.theme

import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import javax.inject.Inject

class ThemeTestViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  private val themeService: ThemeService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource), ThemeTestViewModel {

  override val theme = MutableLiveData<Theme>()

  init {
    themeService.currentTheme.subscribe { theme.value = it }.disposeOnCleared()
  }
}