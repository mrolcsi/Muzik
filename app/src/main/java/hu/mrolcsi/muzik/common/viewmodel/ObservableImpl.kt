package hu.mrolcsi.muzik.common.viewmodel

import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import javax.inject.Inject

class ObservableImpl @Inject constructor() : Observable {

  private val callbackRegistry: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

  override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
    callbackRegistry.add(callback)
  }

  override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
    callbackRegistry.remove(callback)
  }

  fun notifyPropertyChanged(sender: Observable, fieldId: Int) {
    callbackRegistry.notifyCallbacks(sender, fieldId, null)
  }
}