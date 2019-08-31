package hu.mrolcsi.muzik.common.bindings

import android.view.View
import androidx.databinding.BindingConversion

object BindingConverters {

  @JvmStatic
  @BindingConversion
  fun booleanToVisibility(isVisible: Boolean) = if (isVisible) View.VISIBLE else View.GONE

}