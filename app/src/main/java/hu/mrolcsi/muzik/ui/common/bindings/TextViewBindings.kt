package hu.mrolcsi.muzik.ui.common.bindings

import android.text.TextUtils
import android.view.KeyEvent
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

  @JvmStatic
  @BindingAdapter("onEditorAction")
  fun setOnEditorAction(view: TextView, consumer: (Int, KeyEvent?) -> Boolean) {
    view.setOnEditorActionListener { _, actionId, event -> consumer(actionId, event) }
  }

}