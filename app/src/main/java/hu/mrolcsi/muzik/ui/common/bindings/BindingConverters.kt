package hu.mrolcsi.muzik.ui.common.bindings

import android.view.View
import androidx.databinding.BindingConversion

object BindingConverters {

  @JvmStatic
  @BindingConversion
  fun booleanToVisibility(isVisible: Boolean) = if (isVisible) View.VISIBLE else View.GONE

  @JvmStatic
  @BindingConversion
  fun stringToVisibility(text: String?) = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
}