package hu.mrolcsi.android.lyricsplayer.theme

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import androidx.palette.graphics.Palette
import kotlin.math.roundToInt

data class Theme(
  // Source palette
  var sourcePalette: Palette,

  // Basic colors
  var primaryBackgroundColor: Int,
  var primaryForegroundColor: Int,

  var secondaryBackgroundColor: Int,
  var secondaryForegroundColor: Int,

  var tertiaryBackgroundColor: Int,
  var tertiaryForegroundColor: Int
) {

  companion object {
    const val DISABLED_ALPHA = 0.8f
    val DISABLED_OPACITY = (255 * DISABLED_ALPHA).roundToInt()

    fun getRippleDrawable(rippleColor: Int, backgroundColor: Int): RippleDrawable = RippleDrawable(
      ColorStateList.valueOf(rippleColor),
      ColorDrawable(backgroundColor),
      null
    )

    fun getRippleDrawable(rippleColor: Int, background: Drawable? = null) = RippleDrawable(
      ColorStateList.valueOf(rippleColor),
      background,
      null
    )
  }
}