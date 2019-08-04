package hu.mrolcsi.muzik.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Build
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.forEach
import androidx.core.view.get
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import hu.mrolcsi.muzik.service.theme.Theme

fun Activity.applyStatusBarColor(color: Int) {
  window?.statusBarColor = color

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

fun Activity.applyNavigationBarColor(color: Int) {
  window?.navigationBarColor = color

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

fun Toolbar.applyForegroundColor(color: Int) {
  // Icons
  navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
  overflowIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
  menu.forEach { it.icon.setTint(color) }

  // Title and Subtitle
  setTitleTextColor(color)
  setSubtitleTextColor(color)
}

@SuppressLint("RestrictedApi")
fun BottomNavigationView.applyForegroundColor(color: Int) {
  // Selected Colors
  val itemStates = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
  val itemColors = intArrayOf(color, ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY))
  val itemStateList = ColorStateList(itemStates, itemColors)

  itemIconTintList = itemStateList
  itemTextColor = itemStateList

  (this[0] as BottomNavigationMenuView).forEach {
    if (it is BottomNavigationItemView) {
      it.setItemBackground(Theme.getRippleDrawable(color))
    }
  }
}