package hu.mrolcsi.muzik.theme

import android.graphics.Color
import androidx.palette.graphics.Palette
import com.google.gson.annotations.SerializedName
import hu.mrolcsi.muzik.common.Exclude
import hu.mrolcsi.muzik.extensions.toColorHex

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

  @SerializedName("statusBarColor") var statusBarColor: Int = Color.TRANSPARENT
) {

  override fun toString(): String {
    return "Theme(primaryBackgroundColor=${primaryBackgroundColor.toColorHex()}, " +
        "primaryForegroundColor=${primaryForegroundColor.toColorHex()})"
  }

  companion object {
    const val DISABLED_OPACITY = 128
  }

}