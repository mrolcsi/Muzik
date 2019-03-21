package hu.mrolcsi.android.lyricsplayer.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.get
import androidx.lifecycle.MutableLiveData
import androidx.palette.graphics.Palette
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.R
import org.json.JSONObject
import java.util.concurrent.Executors

// Singleton or ViewModel?

class ThemeManager(private val sharedPrefs: SharedPreferences) {


  private val mThemeWorker = Executors.newSingleThreadExecutor()

  private var mPreviousHash = 0
  private val mThemeCache = LruCache<Int, Theme>(50)

  var previousTheme: Theme? = null
  val currentTheme = MutableLiveData<Theme>().apply {
    // Load last used theme from SharedPrefs
    AsyncTask.execute {
      sharedPrefs.getString(LAST_USED_THEME, null)?.let {
        val lastUsedTheme = Theme(JSONObject(it))
        this.postValue(lastUsedTheme)
      }
    }
  }

  private val mPaletteFiler = Palette.Filter { _, hsl ->
    hsl[1] > 0.4
  }

  fun updateFromBitmap(bitmap: Bitmap?) {
    mThemeWorker.submit {

      if (BuildConfig.DEBUG && bitmap == null) {
        Log.v(LOG_TAG, "Ignore null bitmap.")
        return@submit
      }

      val coverArt = bitmap ?: mPlaceholderCoverArt

      // Calculate hash for this bitmap
      val hashCode = coverArt.bitmapHash()

      Log.d(LOG_TAG, "Updating Theme from $bitmap (hash=$hashCode)")

      // compare with hash?
      if (mPreviousHash == hashCode) {
        Log.d(LOG_TAG, "Same bitmap as before. Skipping update.")
        return@submit
      }
      mPreviousHash = hashCode

      // If the Theme is not cached, create it
      if (mThemeCache[hashCode] == null) {
        val mainPalette = Palette.from(coverArt)
          .clearFilters()
          //.addFilter(mPaletteFiler)
          .generate()

        val theme = createTheme(mainPalette)

        val statusBarPalette = Palette.from(coverArt)
          .clearFilters()
          .setRegion(0, 0, coverArt.width, 48)    // approx.
          .generate()

        theme.statusBarColor = statusBarPalette.swatches.maxBy { it.population }?.rgb
          ?: theme.primaryBackgroundColor

        mThemeCache.put(hashCode, theme)
      }

      // Save previous theme
      previousTheme = currentTheme.value
      currentTheme.postValue(mThemeCache[hashCode])

      // Save theme to shared prefs
      sharedPrefs.edit().putString(LAST_USED_THEME, mThemeCache[hashCode].toJson().toString()).apply()
    }
  }

  fun createTheme(sourcePalette: Palette): Theme {

    val swatches = sourcePalette.swatches.sortedByDescending { it.population }

    // Primary colors
    val primaryBackgroundColor = swatches[0]?.rgb ?: Color.BLACK
    val primaryForegroundColor = findForegroundColor(primaryBackgroundColor, sourcePalette)

    val dominantHsl = sourcePalette.dominantSwatch?.hsl
      ?: FloatArray(3).apply {
        ColorUtils.colorToHSL(primaryBackgroundColor, this)
      }

    // Increase luminance for dark colors, decrease for light colors
    //val luminanceDiff = if (dominantHsl[2] < 0.5) (1 - dominantHsl[2]) / 3f else -(dominantHsl[2] / 3f)
    val luminanceDiff = if (dominantHsl[2] < 0.5) 0.1f else -0.1f

    // Secondary colors
    dominantHsl[2] += luminanceDiff
    val secondaryBackgroundColor = try {
      swatches[1]?.rgb ?: ColorUtils.HSLToColor(dominantHsl)
    } catch (e: IndexOutOfBoundsException) {
      ColorUtils.HSLToColor(dominantHsl)
    }
    val secondaryForegroundColor = findForegroundColor(secondaryBackgroundColor, sourcePalette)

    // Tertiary colors
    dominantHsl[2] += 2 * luminanceDiff
    val tertiaryBackgroundColor = try {
      swatches[2]?.rgb ?: ColorUtils.HSLToColor(dominantHsl)
    } catch (e: IndexOutOfBoundsException) {
      ColorUtils.HSLToColor(dominantHsl)
    }
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
      val isAllowed = mPaletteFiler.isAllowed(it.rgb, it.hsl)

      // Also ignore swatches that don't have a minimum contrast
      val hasEnoughContrast = ColorUtils.calculateContrast(it.rgb, backgroundColor) > MINIMUM_CONTRAST_RATIO

      isAllowed && hasEnoughContrast
    }.maxBy {
      //ColorUtils.calculateContrast(it.rgb, backgroundColor)
      //distanceEuclidean(it.rgb, backgroundColor)
      it.population
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

  private fun Bitmap.bitmapHash(): Int {
    val prime = 31 //or a higher prime at your choice
    var hash = prime
    for (x in 0 until width step prime) {
      for (y in 0 until height step prime) {
        hash = prime * hash + this[x, y]
      }
    }
    return hash
  }

  private fun distanceEuclidean(color1: Int, color2: Int): Double {
    // https://en.wikipedia.org/wiki/Color_difference#Euclidean

    val redDistance = Math.pow((Color.red(color2) - Color.red(color1).toDouble()), 2.0)
    val greenDistance = Math.pow((Color.green(color2) - Color.green(color1).toDouble()), 2.0)
    val blueDistance = Math.pow((Color.blue(color2) - Color.blue(color1).toDouble()), 2.0)

    return Math.sqrt(redDistance + greenDistance + blueDistance)
  }

  companion object {
    private const val LOG_TAG = "ThemeManager"

    private const val MINIMUM_CONTRAST_RATIO = 4

    private const val LAST_USED_THEME = "lastUsedTheme"

    private lateinit var mPlaceholderCoverArt: Bitmap

    @Volatile private var instance: ThemeManager? = null

    fun getInstance(context: Context): ThemeManager {
      if (!Companion::mPlaceholderCoverArt.isInitialized) {
        mPlaceholderCoverArt = BitmapFactory.decodeResource(context.resources, R.drawable.placeholder_cover_art)
      }
      return instance ?: ThemeManager(PreferenceManager.getDefaultSharedPreferences(context)).also { instance = it }
    }
  }

}