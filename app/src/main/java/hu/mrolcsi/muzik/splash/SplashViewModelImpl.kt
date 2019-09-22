package hu.mrolcsi.muzik.splash

import android.Manifest
import androidx.databinding.library.baseAdapters.BR
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class SplashViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  private val rxPermissions: RxPermissions
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  SplashViewModel {

  private val requiredPermissions = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
  )

  override var isPermissionRationaleVisible: Boolean by boundBoolean(BR.permissionRationaleVisible)

  private val requestPermissionSubject = PublishSubject.create<Unit>()

  override fun requestPermission() {
    requestPermissionSubject.onNext(Unit)
  }

  init {
    requestPermissionSubject
      .startWith(Unit)
      .switchMap { rxPermissions.requestEachCombined(*requiredPermissions) }
      .subscribeBy { permission ->
        when {
          permission.granted -> sendNavCommand {
            navigate(SplashFragmentDirections.actionToLibrary())
          }
          permission.shouldShowRequestPermissionRationale ->
            isPermissionRationaleVisible = true
        }
      }.disposeOnCleared()
  }
}