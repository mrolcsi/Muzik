package hu.mrolcsi.muzik.common.bindings

import android.graphics.PorterDuff
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import androidx.databinding.BindingAdapter

object ToolbarBindings {

  @JvmStatic
  @BindingAdapter("iconTint")
  fun setIconTint(view: Toolbar, @ColorInt color: Int) {
    view.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    view.overflowIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    view.menu.forEach { it.icon.setTint(color) }
  }

}