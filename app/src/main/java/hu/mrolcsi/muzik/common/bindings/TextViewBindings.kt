package hu.mrolcsi.muzik.common.bindings

import android.widget.TextView
import androidx.core.view.postDelayed
import androidx.databinding.BindingAdapter

object TextViewBindings {

  @JvmStatic
  @BindingAdapter(value = ["enableMarquee", "marqueeDelay"], requireAll = false)
  fun setMarqueeEnabled(view: TextView, enableMarquee: Boolean?, marqueeDelay: Int?) {
    if (enableMarquee == true) {
      val delay = marqueeDelay?.toLong() ?: 0
      if (delay > 0) {
        view.postDelayed(delay) {
          view.isSelected = true
        }
      } else {
        view.isSelected = true
      }
    } else {
      view.isSelected = false
    }
  }

}