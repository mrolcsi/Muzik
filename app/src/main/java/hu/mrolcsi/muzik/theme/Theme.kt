package hu.mrolcsi.muzik.theme

import android.graphics.Color
import androidx.palette.graphics.Palette
import com.google.gson.annotations.SerializedName
import hu.mrolcsi.muzik.common.Exclude
import hu.mrolcsi.muzik.extensions.toColorHex
import kotlin.math.roundToInt

data class Theme(
  // Source palette
  @Exclude var sourcePalette: Palette? = null,

  // Basic colors
  @SerializedName("primaryBackgroundColor") val primaryBackgroundColor: Int,
  @SerializedName("primaryForegroundColor") val primaryForegroundColor: Int,
  @SerializedName("secondaryBackgroundColor") val secondaryBackgroundColor: Int,
  @SerializedName("secondaryForegroundColor") val secondaryForegroundColor: Int,
  @SerializedName("tertiaryBackgroundColor") val tertiaryBackgroundColor: Int,
  @SerializedName("tertiaryForegroundColor") val tertiaryForegroundColor: Int,

  @Deprecated("use primaryBackgroundColor")
  @SerializedName("statusBarColor") var statusBarColor: Int = Color.TRANSPARENT
) {

  override fun toString(): String {
    return "Theme(primaryBackgroundColor=${primaryBackgroundColor.toColorHex()}, " +
        "primaryForegroundColor=${primaryForegroundColor.toColorHex()})"
  }

  companion object {
    val DISABLED_OPACITY = (255 * 0.6).roundToInt()

    val DEFAULT_THEME = Theme(
      sourcePalette = null,
      primaryBackgroundColor = Color.rgb(39, 115, 231),
      primaryForegroundColor = Color.WHITE,
      secondaryBackgroundColor = Color.BLACK,
      secondaryForegroundColor = Color.WHITE,
      tertiaryBackgroundColor = Color.rgb(46, 70, 106),
      tertiaryForegroundColor = Color.WHITE
    )
  }

}