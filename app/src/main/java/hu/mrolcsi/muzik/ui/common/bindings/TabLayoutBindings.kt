package hu.mrolcsi.muzik.ui.common.bindings

import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.databinding.BindingAdapter
import com.google.android.material.tabs.TabLayout
import hu.mrolcsi.muzik.ui.common.extensions.DISABLED_OPACITY

object TabLayoutBindings {

  @JvmStatic
  @BindingAdapter("tabTextColor")
  fun setTabTextColor(view: TabLayout, @ColorInt color: Int) {
    view.setTabTextColors(
      ColorUtils.setAlphaComponent(color, DISABLED_OPACITY),
      color
    )
  }

  @JvmStatic
  @BindingAdapter("tabIndicatorColor")
  fun setTabIndicatorColor(view: TabLayout, @ColorInt color: Int) {
    view.setSelectedTabIndicatorColor(color)
  }

}