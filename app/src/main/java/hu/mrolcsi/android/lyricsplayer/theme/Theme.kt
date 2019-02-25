package hu.mrolcsi.android.lyricsplayer.theme

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import androidx.palette.graphics.Palette
import kotlin.math.roundToInt

data class Theme(
  // Source palette
  var palette: Palette,

  // Basic colors
  var backgroundColor: Int,
  var foregroundColor: Int,

  var darkBackgroundColor: Int,
  var darkForegroundColor: Int,

  var darkerBackgroundColor: Int,
  var darkerForegroundColor: Int
) {

  fun getRippleDrawable(pressedColor: Int, normalColor: Int): RippleDrawable = RippleDrawable(
    ColorStateList.valueOf(pressedColor),
    ColorDrawable(normalColor),
    null
  )

  companion object {
    val INACTIVE_OPACITY = (255 * 0.9).roundToInt()
  }
}