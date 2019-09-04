package hu.mrolcsi.muzik.common.bindings

import android.text.TextUtils
import android.widget.TextView
import androidx.core.view.postDelayed
import androidx.databinding.BindingAdapter

object TextViewBindings {

  @JvmStatic
  @BindingAdapter(value = ["enableMarquee", "marqueeDelay"], requireAll = false)
  fun setMarqueeEnabled(view: TextView, enableMarquee: Boolean?, marqueeDelay: Int?) {

    if (enableMarquee == true) {
      view.setSingleLine(true)
      view.ellipsize = TextUtils.TruncateAt.MARQUEE
      view.marqueeRepeatLimit = -1

      val delay = marqueeDelay?.toLong() ?: 0
      if (delay > 0) {
        view.postDelayed(delay) {
          view.isSelected = true
        }
      } else {
        view.isSelected = true
      }
    } else {
      view.setSingleLine(false)
      view.ellipsize = null
    }
  }

}