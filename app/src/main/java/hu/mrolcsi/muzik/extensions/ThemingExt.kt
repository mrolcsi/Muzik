package hu.mrolcsi.muzik.extensions

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.graphics.ColorUtils

const val DISABLED_ALPHA = 0.5f
const val DISABLED_OPACITY = (255 * DISABLED_ALPHA).toInt()

fun Activity.updateStatusBarIcons(color: Int) {
//  window?.statusBarColor = color

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    window?.decorView?.apply {
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

fun Activity.updateNavigationIcons(color: Int) {
//  window?.navigationBarColor = color

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    window?.decorView?.apply {
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