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

    const val PREFERRED_ANIMATION_DURATION: Long = 300

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Theme) return false

    if (primaryBackgroundColor != other.primaryBackgroundColor) return false
    if (primaryForegroundColor != other.primaryForegroundColor) return false
    if (secondaryBackgroundColor != other.secondaryBackgroundColor) return false
    if (secondaryForegroundColor != other.secondaryForegroundColor) return false
    if (tertiaryBackgroundColor != other.tertiaryBackgroundColor) return false
    if (tertiaryForegroundColor != other.tertiaryForegroundColor) return false

    return true
  }

  override fun hashCode(): Int {
    var result = primaryBackgroundColor
    result = 31 * result + primaryForegroundColor
    result = 31 * result + secondaryBackgroundColor
    result = 31 * result + secondaryForegroundColor
    result = 31 * result + tertiaryBackgroundColor
    result = 31 * result + tertiaryForegroundColor
    return result
  }
}