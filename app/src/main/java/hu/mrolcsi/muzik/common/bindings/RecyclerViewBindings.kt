package hu.mrolcsi.muzik.common.bindings

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration

object RecyclerViewBindings {

  @JvmStatic
  @BindingAdapter(value = ["showColoredDivider", "dividerColor", "dividerDrawable"], requireAll = false)
  fun setShowColoredDivider(
    view: RecyclerView,
    showColoredDivider: Boolean, @ColorInt color: Int?,
    drawable: Drawable?
  ) {
    if (showColoredDivider) {
      for (i in 0 until view.itemDecorationCount) view.removeItemDecorationAt(i)
      view.addItemDecoration(ColoredDividerItemDecoration(view.context, DividerItemDecoration.VERTICAL).apply {
        drawable?.let { setDrawable(drawable) }
        color?.let { setTint(it) }
      })
    }
  }
}