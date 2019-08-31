package hu.mrolcsi.muzik.common.bindings

import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration

object RecyclerViewBindings {

  @JvmStatic
  @BindingAdapter("showColoredDivider")
  fun setShowColoredDivider(view: RecyclerView, showColoredDivider: Boolean) {
    if (showColoredDivider) {
      view.addItemDecoration(ColoredDividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
    }
  }

  @JvmStatic
  @BindingAdapter("dividerColor")
  fun setDividerColor(view: RecyclerView, @ColorInt color: Int) {
    for (i in 0 until view.itemDecorationCount) {
      view.getItemDecorationAt(i).let {
        (it as ColoredDividerItemDecoration).setTint(
          color
          //ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY)
        )
      }
    }
  }

}