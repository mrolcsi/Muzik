package hu.mrolcsi.muzik.ui.base

import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import androidx.databinding.Observable as DataBindingObservable

interface PermissionViewModel : DataBindingObservable {

  @get:Bindable
  val isPermissionRationaleVisible: Boolean

  fun <T> requirePermissions(permissions: Array<String>, onPermissionGranted: () -> Observable<T>): Observable<T>

  fun onRequestPermissionClicked()
}

open class RxPermissionViewModel(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  private val rxPermissions: RxPermissions
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  PermissionViewModel {

  override var isPermissionRationaleVisible: Boolean by boundBoolean(BR.permissionRationaleVisible)

  private val requestPermissionSubject = PublishSubject.create<Unit>()

  override fun onRequestPermissionClicked() {
    requestPermissionSubject.onNext(Unit)
  }

  override fun <T> requirePermissions(
    permissions: Array<String>,
    onPermissionGranted: () -> Observable<T>
  ): Observable<T> =
    requestPermissionSubject
      .startWith(Unit)
      .switchMap { rxPermissions.requestEachCombined(*permissions) }
      .doOnNext { isPermissionRationaleVisible = it.shouldShowRequestPermissionRationale }
      .filter { it.granted }
      .switchMap { onPermissionGranted() }
}