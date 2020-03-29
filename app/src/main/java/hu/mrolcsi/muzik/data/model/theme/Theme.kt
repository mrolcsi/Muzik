package hu.mrolcsi.muzik.data.model.theme

import android.graphics.Color
import androidx.palette.graphics.Palette
import com.google.gson.annotations.SerializedName
import hu.mrolcsi.muzik.data.remote.Exclude
import hu.mrolcsi.muzik.ui.common.extensions.toColorHex
import kotlin.math.roundToInt

data class Theme(
  // Source palette
  @Exclude var sourcePalette: Palette? = null,

  // Basic colors
  @SerializedName("primaryBackgroundColor") val backgroundColor: Int,
  @SerializedName("primaryForegroundColor") val foregroundColor: Int
) {

  override fun toString(): String =
    "Theme(backgroundColor=${backgroundColor.toColorHex()}, foregroundColor=${foregroundColor.toColorHex()})"

  companion object {
    val DISABLED_OPACITY = (255 * 0.6).roundToInt()

    val DEFAULT_THEME = Theme(
      sourcePalette = null,
      backgroundColor = Color.rgb(39, 115, 231),
      foregroundColor = Color.WHITE
    )
  }
}