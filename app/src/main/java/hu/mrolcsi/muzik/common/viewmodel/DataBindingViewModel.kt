package hu.mrolcsi.muzik.common.viewmodel

import androidx.databinding.Observable
import com.afollestad.materialdialogs.MaterialDialog
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

  open fun showError(throwable: Throwable) = sendUiCommand {
    MaterialDialog(this)
      .show {
        positiveButton(android.R.string.ok)
        message(text = throwable.localizedMessage)
      }
  }
}