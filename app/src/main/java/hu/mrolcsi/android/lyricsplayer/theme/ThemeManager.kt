package hu.mrolcsi.android.lyricsplayer.theme

import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.MutableLiveData
import androidx.palette.graphics.Palette

// Singleton or ViewModel?

object ThemeManager {

  private const val LOG_TAG = "ThemeManager"

  var previousTheme: Theme? = null
  val currentTheme = MutableLiveData<Theme>()

  private val mPaletteFiler = Palette.Filter { _, hsl ->
    hsl[2] > 0.01 && hsl[2] < 0.99
  }

  fun updateFromBitmap(bitmap: Bitmap) {
    AsyncTask.execute {
      Log.d(LOG_TAG, "Updating Theme...")

      val palette = Palette.from(bitmap)
        .clearFilters()
        //.addFilter(mPaletteFiler)
        .generate()

      val theme = createTheme(palette)

      // Save previous theme
      previousTheme = currentTheme.value
      currentTheme.postValue(theme)
    }
  }

  private fun createTheme(palette: Palette): Theme {

    // Default colors
    val backgroundColor = palette.dominantSwatch?.rgb ?: Color.BLACK
    val foregroundColor = palette.swatches.maxBy {
      ColorUtils.calculateContrast(it.rgb, backgroundColor)
    }?.rgb ?: Color.WHITE
    val backgroundHsl = FloatArray(3).apply {
      ColorUtils.colorToHSL(backgroundColor, this)
    }

    // Darker colors
    backgroundHsl[2] *= 0.6f
    val darkBackgroundColor = ColorUtils.HSLToColor(backgroundHsl)
    val darkForegroundColor = palette.swatches.maxBy {
      ColorUtils.calculateContrast(it.rgb, darkBackgroundColor)
    }?.rgb ?: Color.WHITE

    // Even darker colors
    backgroundHsl[2] *= 0.6f
    val darkerBackgroundColor = ColorUtils.HSLToColor(backgroundHsl)
    val darkerForegroundColor = palette.swatches.maxBy {
      ColorUtils.calculateContrast(it.rgb, darkerBackgroundColor)
    }?.rgb ?: Color.WHITE

    return Theme(
      palette,
      backgroundColor,
      foregroundColor,
      darkBackgroundColor,
      darkForegroundColor,
      darkerBackgroundColor,
      darkerForegroundColor
    )
  }
}