package hu.mrolcsi.muzik.extensions

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.theme.Theme

const val DISABLED_ALPHA = 0.5f
const val DISABLED_OPACITY = (255 * DISABLED_ALPHA).toInt()

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

@Deprecated("Use 'app:iconTint' with DataBinding!")
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
  val itemColors = intArrayOf(color, ColorUtils.setAlphaComponent(color, DISABLED_OPACITY))
  val itemStateList = ColorStateList(itemStates, itemColors)

  itemIconTintList = itemStateList
  itemTextColor = itemStateList

  (this[0] as BottomNavigationMenuView).forEach {
    if (it is BottomNavigationItemView) {
      it.setItemBackground(getRippleDrawable(color))
    }
  }
}

fun getRippleDrawable(rippleColor: Int, backgroundColor: Int): RippleDrawable = RippleDrawable(
  ColorStateList.valueOf(rippleColor),
  null,
  ColorDrawable(backgroundColor)
)

fun getRippleDrawable(rippleColor: Int, background: Drawable? = null) = RippleDrawable(
  ColorStateList.valueOf(rippleColor),
  background,
  null
)

fun Fragment.applyThemeAnimated(
  previousTheme: Theme?, newTheme: Theme,
  applyPrimaryBackgroundColor: ((color: Int) -> Unit)? = null,
  applyPrimaryForegroundColor: ((color: Int) -> Unit)? = null,
  applySecondaryBackgroundColor: ((color: Int) -> Unit)? = null,
  applySecondaryForegroundColor: ((color: Int) -> Unit)? = null
) {
  val animationDuration = requireContext().resources.getInteger(R.integer.preferredAnimationDuration).toLong()

  applyPrimaryBackgroundColor?.let { apply ->
    ValueAnimator.ofArgb(
      previousTheme?.primaryBackgroundColor ?: Theme.DEFAULT_THEME.primaryBackgroundColor,
      newTheme.primaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        apply.invoke(color)
      }
      start()
    }
  }

  applyPrimaryForegroundColor?.let { apply ->
    ValueAnimator.ofArgb(
      previousTheme?.primaryForegroundColor ?: Theme.DEFAULT_THEME.primaryForegroundColor,
      newTheme.primaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        apply.invoke(color)
      }
      start()
    }
  }

  applySecondaryBackgroundColor?.let { apply ->
    ValueAnimator.ofArgb(
      previousTheme?.secondaryBackgroundColor ?: Theme.DEFAULT_THEME.secondaryBackgroundColor,
      newTheme.secondaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        apply.invoke(color)
      }
      start()
    }
  }

  applySecondaryForegroundColor?.let { apply ->
    ValueAnimator.ofArgb(
      previousTheme?.secondaryForegroundColor ?: Theme.DEFAULT_THEME.secondaryForegroundColor,
      newTheme.secondaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        apply.invoke(color)
      }
      start()
    }
  }
}