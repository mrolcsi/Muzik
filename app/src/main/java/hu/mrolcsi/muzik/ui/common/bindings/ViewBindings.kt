package hu.mrolcsi.muzik.ui.common.bindings

import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter

object ViewBindings {

  @BindingAdapter("rippleColor")
  @JvmStatic
  fun setRippleColor(view: View, @ColorInt color: Int) {
    (view.background as? RippleDrawable)?.setColor(ColorStateList.valueOf(color))
  }

}