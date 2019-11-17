package hu.mrolcsi.muzik.ui.base

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class RxViewModel : ViewModel() {

  private val disposables = CompositeDisposable()

  fun Disposable.disposeOnCleared() = disposables.add(this)

  public override fun onCleared() {
    super.onCleared()
    disposables.clear()
  }
}