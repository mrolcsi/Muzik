package hu.mrolcsi.muzik.common.viewmodel

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

abstract class RxViewModel : ViewModel() {

  private val disposables = CompositeDisposable()

  private val cleared = BehaviorSubject.create<Boolean>()
  val active get() = cleared.value != true

  fun <T> Observable<T>.takeUntilCleared(): Observable<T> = takeUntil(cleared)

  fun Disposable.disposeOnClear() = disposables.add(this)

  public override fun onCleared() {
    super.onCleared()
    cleared.onNext(true)
    disposables.clear()
  }
}