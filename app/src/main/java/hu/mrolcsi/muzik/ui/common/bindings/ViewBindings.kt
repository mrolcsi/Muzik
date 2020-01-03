package hu.mrolcsi.muzik.ui.common.bindings

import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import kotlin.math.roundToInt

object ViewBindings {

  @BindingAdapter("rippleColor")
  @JvmStatic
  fun setRippleColor(view: View, @ColorInt color: Int) {
    (view.background as? RippleDrawable)?.setColor(ColorStateList.valueOf(color))
  }

  @JvmStatic
  @BindingAdapter(
    value = ["android:layout_marginStart", "android:layout_marginTop", "android:layout_marginEnd", "android:layout_marginBottom"],
    requireAll = false
  )
  fun setMargins(
    view: View,
    marginStart: Float = 0f,
    marginTop: Float = 0f,
    marginEnd: Float = 0f,
    marginBottom: Float = 0f
  ) {
    view.updateLayoutParams {
      (this as? ViewGroup.MarginLayoutParams)?.let {
        it.setMargins(
          if (marginStart > 0) marginStart.roundToInt() else it.leftMargin,
          if (marginTop > 0) marginTop.roundToInt() else it.topMargin,
          if (marginEnd > 0) marginEnd.roundToInt() else it.rightMargin,
          if (marginBottom > 0) marginBottom.roundToInt() else it.bottomMargin
        )
      }
    }
  }

}