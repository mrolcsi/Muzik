package hu.mrolcsi.muzik.splash

import androidx.databinding.Bindable
import androidx.databinding.Observable
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface SplashViewModel : Observable, NavCommandSource, ThemedViewModel {

  @get:Bindable
  val isPermissionRationaleVisible: Boolean

  fun requestPermission()
}