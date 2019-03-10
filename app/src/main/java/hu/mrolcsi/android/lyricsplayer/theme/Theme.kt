package hu.mrolcsi.android.lyricsplayer.theme

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import androidx.palette.graphics.Palette

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
    const val SUBTITLE_ALPHA = 0.8f
    const val SUBTITLE_OPACITY = (255 * SUBTITLE_ALPHA).toInt()

    const val DISABLED_ALPHA = 0.5f
    const val DISABLED_OPACITY = (255 * DISABLED_ALPHA).toInt()

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
  }
}