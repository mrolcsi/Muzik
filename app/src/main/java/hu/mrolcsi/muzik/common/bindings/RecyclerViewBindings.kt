package hu.mrolcsi.muzik.common.bindings

import android.graphics.drawable.Drawable
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
      for (i in 0 until view.itemDecorationCount) view.removeItemDecorationAt(i)
      view.addItemDecoration(ColoredDividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
    }
  }

  @JvmStatic
  @BindingAdapter("dividerColor")
  fun setDividerColor(view: RecyclerView, @ColorInt color: Int) {
    for (i in 0 until view.itemDecorationCount) {
      view.getItemDecorationAt(i).let {
        (it as? ColoredDividerItemDecoration)?.setTint(color)
      }
    }
  }

  @JvmStatic
  @BindingAdapter("dividerDrawable")
  fun setDividerDrawable(view: RecyclerView, drawable: Drawable) {
    for (i in 0 until view.itemDecorationCount) {
      view.getItemDecorationAt(i).let {
        (it as? ColoredDividerItemDecoration)?.setDrawable(drawable)
      }
    }
  }
}