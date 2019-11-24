package hu.mrolcsi.muzik.ui.base

import androidx.databinding.Bindable
import androidx.databinding.Observable
import hu.mrolcsi.muzik.ui.common.LiveEvent

interface PermissionViewModel : Observable {

  @get:Bindable
  val isPermissionRationaleVisible: Boolean

  val requestPermissionEvent: LiveEvent<Array<String>>

  fun requestPermission()

  fun onPermissionGranted()

  fun onPermissionDenied(shouldShowPermissionRationale: Boolean)
}