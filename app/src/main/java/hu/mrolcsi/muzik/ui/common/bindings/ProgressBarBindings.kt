package hu.mrolcsi.muzik.ui.common.bindings

import android.content.res.ColorStateList
import android.widget.ProgressBar
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter

object ProgressBarBindings {

  @JvmStatic
  @BindingAdapter("android:progressBackgroundTint")
  fun setProgressBackgroundTint(view: ProgressBar, @ColorInt tintColor: Int) {
    view.progressBackgroundTintList = ColorStateList.valueOf(tintColor)
  }

}