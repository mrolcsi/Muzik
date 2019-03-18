package hu.mrolcsi.android.lyricsplayer.theme

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import androidx.palette.graphics.Palette
import hu.mrolcsi.android.lyricsplayer.extensions.toColorHex
import org.json.JSONObject

data class Theme(
  // Source palette
  var sourcePalette: Palette? = null,

  // Basic colors
  var primaryBackgroundColor: Int,
  var primaryForegroundColor: Int,

  var secondaryBackgroundColor: Int,
  var secondaryForegroundColor: Int,

  var tertiaryBackgroundColor: Int,
  var tertiaryForegroundColor: Int,

  var statusBarColor: Int = Color.TRANSPARENT
) {

  constructor(json: JSONObject) : this(
    null,
    json.getInt(PRIMARY_BACKGROUND),
    json.getInt(PRIMARY_FOREGROUND),
    json.getInt(SECONDARY_BACKGROUND),
    json.getInt(SECONDARY_FOREGROUND),
    json.getInt(TERTIARY_BACKGROUND),
    json.getInt(TERTIARY_FOREGROUND),
    json.getInt(STATUS_BAR_COLOR)
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Theme) return false

    if (primaryBackgroundColor != other.primaryBackgroundColor) return false
    if (primaryForegroundColor != other.primaryForegroundColor) return false
    if (secondaryBackgroundColor != other.secondaryBackgroundColor) return false
    if (secondaryForegroundColor != other.secondaryForegroundColor) return false
    if (tertiaryBackgroundColor != other.tertiaryBackgroundColor) return false
    if (tertiaryForegroundColor != other.tertiaryForegroundColor) return false
    if (statusBarColor != other.statusBarColor) return false

    return true
  }

  override fun hashCode(): Int {
    var result = primaryBackgroundColor
    result = 31 * result + primaryForegroundColor
    result = 31 * result + secondaryBackgroundColor
    result = 31 * result + secondaryForegroundColor
    result = 31 * result + tertiaryBackgroundColor
    result = 31 * result + tertiaryForegroundColor
    result = 31 * result + statusBarColor
    return result
  }

  override fun toString(): String {
    return "Theme(primaryBackgroundColor=${primaryBackgroundColor.toColorHex()}, " +
        "primaryForegroundColor=${primaryForegroundColor.toColorHex()})"
  }

  fun toJson(): JSONObject {
    val json = JSONObject()
    json.put(PRIMARY_BACKGROUND, primaryBackgroundColor)
    json.put(PRIMARY_FOREGROUND, primaryForegroundColor)
    json.put(SECONDARY_BACKGROUND, secondaryBackgroundColor)
    json.put(SECONDARY_FOREGROUND, secondaryForegroundColor)
    json.put(TERTIARY_BACKGROUND, tertiaryBackgroundColor)
    json.put(TERTIARY_FOREGROUND, tertiaryForegroundColor)
    json.put(STATUS_BAR_COLOR, statusBarColor)
    return json
  }

  companion object {
    const val SUBTITLE_ALPHA = 0.8f
    const val SUBTITLE_OPACITY = (255 * SUBTITLE_ALPHA).toInt()

    const val DISABLED_ALPHA = 0.5f
    const val DISABLED_OPACITY = (255 * DISABLED_ALPHA).toInt()

    const val PREFERRED_ANIMATION_DURATION: Long = 300

    // JSON Keys
    const val PRIMARY_BACKGROUND = "primaryBackground"
    const val PRIMARY_FOREGROUND = "primaryForeground"
    const val SECONDARY_BACKGROUND = "secondaryBackground"
    const val SECONDARY_FOREGROUND = "secondaryForeground"
    const val TERTIARY_BACKGROUND = "tertiaryBackground"
    const val TERTIARY_FOREGROUND = "tertiaryForeground"
    const val STATUS_BAR_COLOR = "statusBarColor"

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