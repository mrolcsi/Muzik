package hu.mrolcsi.android.lyricsplayer.theme

import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.os.Looper
import android.util.Log
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.MutableLiveData
import androidx.palette.graphics.Palette

// Singleton or ViewModel?

object ThemeManager {

  private const val LOG_TAG = "ThemeManager"

  private const val MINIMUM_CONTRAST_RATIO = 4.5

  var previousTheme: Theme? = null
  val currentTheme = MutableLiveData<Theme>()

  private val mPaletteFiler = Palette.Filter { _, hsl ->
    hsl[2] > 0.10 && hsl[2] < 0.90
  }

  fun updateFromBitmap(bitmap: Bitmap) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      AsyncTask.execute {
        updateFromBitmap(bitmap)
      }
    } else {
      Log.d(LOG_TAG, "Updating Theme from $bitmap")

      val palette = Palette.from(bitmap)
        .clearFilters()
        //.addFilter(mPaletteFiler)
        .generate()

      if (palette == previousTheme?.sourcePalette) {
        // Skip theme generation from the same(ish) palette.
        return
      }

      val theme = createTheme(palette)

      // Save previous theme
      previousTheme = currentTheme.value
      currentTheme.postValue(theme)
    }
  }

  private fun createTheme(sourcePalette: Palette): Theme {

    // Primary colors
    val primaryBackgroundColor = sourcePalette.dominantSwatch?.rgb ?: Color.BLACK
    val primaryForegroundColor = findForegroundColor(primaryBackgroundColor, sourcePalette)

    val dominantHsl = sourcePalette.dominantSwatch?.hsl
      ?: FloatArray(3).apply {
        ColorUtils.colorToHSL(primaryBackgroundColor, this)
      }

    /*

     Light
     |       T       S       P                |
     |       |       |       |                |
    -+-------x-------x---+---x----------------+-
     |                   |                    |
     0                   0.5                  1

     Dark
     |       P          S          T          |
     |       |          |          |          |
    -+-------x----------x+---------x----------+-
     |                   |                    |
     0                   0.5                  1

    diff = if (l < 0.5) {
      (1 - l) / 3
    } else {
      -(l / 3)
    }

     */

    // Increase luminance for dark colors, decrease for light colors
    //val luminanceDiff = if (dominantHsl[2] < 0.5) (1 - dominantHsl[2]) / 3f else -(dominantHsl[2] / 3f)
    val luminanceDiff = if (dominantHsl[2] < 0.5) 0.1f else -0.1f

    // Secondary colors
    dominantHsl[2] += luminanceDiff
    val secondaryBackgroundColor = ColorUtils.HSLToColor(dominantHsl)
    val secondaryForegroundColor = findForegroundColor(secondaryBackgroundColor, sourcePalette)

    // Tertiary colors
    dominantHsl[2] += 2 * luminanceDiff
    val tertiaryBackgroundColor = ColorUtils.HSLToColor(dominantHsl)
    val tertiaryForegroundColor = findForegroundColor(tertiaryBackgroundColor, sourcePalette)

    return Theme(
      sourcePalette,
      primaryBackgroundColor,
      primaryForegroundColor,
      secondaryBackgroundColor,
      secondaryForegroundColor,
      tertiaryBackgroundColor,
      tertiaryForegroundColor
    )
  }

  private fun findForegroundColor(backgroundColor: Int, sourcePalette: Palette): Int {
    var foregroundColor = sourcePalette.swatches.filter {

      // Ignore completely black or completely white swatches
      val notBlackOrWhite = mPaletteFiler.isAllowed(it.rgb, it.hsl)

      // Also ignore swatches that don't have a minimum contrast
      val hasEnoughContrast = ColorUtils.calculateContrast(it.rgb, backgroundColor) > MINIMUM_CONTRAST_RATIO

      notBlackOrWhite && hasEnoughContrast
    }.maxBy {
      ColorUtils.calculateContrast(it.rgb, backgroundColor)
    }?.rgb

    if (foregroundColor == null) {
      // Use first available color
      foregroundColor = sourcePalette.swatches.maxBy {
        ColorUtils.calculateContrast(it.rgb, backgroundColor)
      }?.rgb ?:
          // Use inverse of background color as a fallback
          invertColor(backgroundColor)
    }

    return foregroundColor
  }

  private fun invertColor(color: Int): Int = color xor 0x00ffffff
}