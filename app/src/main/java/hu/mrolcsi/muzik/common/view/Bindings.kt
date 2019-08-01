package hu.mrolcsi.muzik.common.view

import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

object BindingAdapters {

  @JvmStatic
  @BindingAdapter("isRefreshing")
  fun setIsRefreshing(view: SwipeRefreshLayout, isRefreshing: Boolean) {
    view.isRefreshing = isRefreshing
  }

}
