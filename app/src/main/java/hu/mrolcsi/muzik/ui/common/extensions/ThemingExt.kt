package hu.mrolcsi.muzik.ui.common.extensions

import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.graphics.ColorUtils

const val DISABLED_ALPHA = 0.5f
const val DISABLED_OPACITY = (255 * DISABLED_ALPHA).toInt()

fun Window.updateStatusBarIcons(color: Int) {

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    decorView.apply {
      val flags = systemUiVisibility
      systemUiVisibility =
        if (ColorUtils.calculateLuminance(color) < 0.5) {
          // Clear flag (white icon)
          flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
          // Set flag (gray icons)
          flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
  }
}

fun Window.updateNavigationIcons(color: Int) {
//  window?.navigationBarColor = color

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    decorView.apply {
      val flags = systemUiVisibility
      systemUiVisibility =
        if (ColorUtils.calculateLuminance(color) < 0.5) {
          // Clear flag (white icons)
          flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        } else {
          // Set flag (gray icons)
          flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
  }
}