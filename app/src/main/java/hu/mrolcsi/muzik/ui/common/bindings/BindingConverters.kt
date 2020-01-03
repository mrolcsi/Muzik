package hu.mrolcsi.muzik.ui.common.bindings

import android.net.Uri
import android.view.View
import androidx.databinding.BindingConversion

object BindingConverters {

  @BindingConversion
  @JvmStatic
  fun booleanToVisibility(isVisible: Boolean): Int = if (isVisible) View.VISIBLE else View.GONE

  @BindingConversion
  @JvmStatic
  fun textToVisibility(text: CharSequence?): Int = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE

  @BindingConversion
  @JvmStatic
  fun uriToString(uri: Uri?): String? = uri?.toString()
}