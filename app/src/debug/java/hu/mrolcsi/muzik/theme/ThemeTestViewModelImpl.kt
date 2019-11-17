package hu.mrolcsi.muzik.theme

import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.data.model.theme.Theme
import hu.mrolcsi.muzik.data.service.theme.ThemeService

class ThemeTestViewModelImpl constructor(
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