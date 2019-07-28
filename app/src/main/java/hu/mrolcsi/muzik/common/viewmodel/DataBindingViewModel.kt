package hu.mrolcsi.muzik.common.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.databinding.Observable
import com.afollestad.materialdialogs.MaterialDialog
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun containsNullOrBlank(vararg strings: String?) = strings.any { it.isNullOrBlank() }

open class DataBindingViewModel(
  private val observable: ObservableImpl,
  private val uiCommandSource: ExecuteOnceUiCommandSource
) : RxViewModel(), Observable by observable, UiCommandSource by uiCommandSource {

  inner class BoundProperty<T>(initialValue: T, private val onNewValue: ((T) -> Unit)?, private val id: Int) :
    ObservableProperty<T>(initialValue) {

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
      if (oldValue != newValue) {
        onNewValue?.invoke(newValue)
        notifyPropertyChanged(id)
      }
    }
  }

  fun boundBoolean(
    id: Int,
    initialValue: Boolean = false,
    onNewValue: ((Boolean) -> Unit)? = null
  ): ReadWriteProperty<Any?, Boolean> {
    return BoundProperty(initialValue, onNewValue, id)
  }

  fun boundString(
    id: Int,
    initialValue: String = "",
    onNewValue: ((String) -> Unit)? = null
  ): ReadWriteProperty<Any?, String> {
    return BoundProperty(initialValue, onNewValue, id)
  }

  fun boundStringOrNull(
    id: Int,
    initialValue: String? = null,
    onNewValue: ((String?) -> Unit)? = null
  ): ReadWriteProperty<Any?, String?> {
    return BoundProperty(initialValue, onNewValue, id)
  }

  fun boundInt(
    id: Int,
    initialValue: Int,
    onNewValue: ((Int) -> Unit)? = null
  ): ReadWriteProperty<Any?, Int> {
    return BoundProperty(initialValue, onNewValue, id)
  }

  fun boundCharSequence(
    id: Int,
    initialValue: CharSequence = "",
    onNewValue: ((CharSequence) -> Unit)? = null
  ): ReadWriteProperty<Any?, CharSequence> {
    return BoundProperty(initialValue, onNewValue, id)
  }

  fun <T> boundProperty(
    id: Int,
    initialValue: T,
    onNewValue: ((T) -> Unit)? = null
  ): ReadWriteProperty<Any?, T> {
    return BoundProperty(initialValue, onNewValue, id)
  }

  fun notifyPropertyChanged(fieldId: Int) = observable.notifyPropertyChanged(this, fieldId)

  fun finishActivity() = setUiCommand { finish() }

  fun startActivity(intent: Activity.() -> Intent) =
    setUiCommand { startActivity(intent()) }

  fun startActivityForResult(requestCode: Int, intent: Activity.() -> Intent) =
    setUiCommand { startActivityForResult(intent(), requestCode) }

  fun setUiCommand(command: UiCommand) {
    uiCommandSource.uiCommand.value = command
  }

  open fun showError(throwable: Throwable) = setUiCommand {
    MaterialDialog(this)
      .show {
        positiveButton(android.R.string.ok)
        message(text = throwable.localizedMessage)
      }
  }
}