package hu.mrolcsi.muzik.ui.base

import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class RxFragment : Fragment() {

  private val disposableBag = CompositeDisposable()

  override fun onDestroy() {
    disposableBag.dispose()
    super.onDestroy()
  }

  protected fun Disposable.disposeOnDestroy() {
    disposableBag.add(this)
  }
}