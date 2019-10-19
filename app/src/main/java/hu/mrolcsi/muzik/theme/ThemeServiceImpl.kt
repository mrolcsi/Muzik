package hu.mrolcsi.muzik.theme

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.get
import androidx.palette.graphics.Palette
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("CheckResult")
class ThemeServiceImpl : ThemeService, KoinComponent {

  private val sharedPrefs: SharedPreferences by inject()
  private val gson: Gson by inject()

  private val pendingThemeSubject: Subject<Observable<Theme>> = PublishSubject.create()
  private val createThemeSubject: Subject<Bitmap> = PublishSubject.create()

  private val themeCache = LruCache<Int, Theme>(50)

  override val currentTheme: Observable<Theme>

  private val mPaletteFiler = Palette.Filter { _, hsl -> hsl[1] > 0.4 }

  init {
    currentTheme = pendingThemeSubject
      .switchMap { it }
      .doOnNext { Log.d(LOG_TAG, "Theme ready: $it") }
      .replay(1)
      .apply { connect() }
      .hide()
      .observeOn(AndroidSchedulers.mainThread())

    createThemeSubject
      .observeOn(Schedulers.computation())
      .distinctUntilChanged { bitmap -> bitmap.bitmapHash() }
      .flatMapSingle { createTheme(it) }
      .doOnNext { Log.d(LOG_TAG, "Updating theme: $it") }
      .doOnNext { sharedPrefs.edit().putString(LAST_USED_THEME, gson.toJson(it)).apply() }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { pendingThemeSubject.onNext(Observable.just(it)) },
        onError = { Log.e(LOG_TAG, Log.getStackTraceString(it)) }
      )

    loadSavedTheme()
  }

  private fun loadSavedTheme() {
    val savedTheme = sharedPrefs.getString(LAST_USED_THEME, null)?.let {
      gson.fromJson(it, Theme::class.java)
    }

    pendingThemeSubject.onNext(Observable.just(savedTheme ?: Theme.DEFAULT_THEME))
  }

  override fun updateTheme(bitmap: Bitmap) {
    createThemeSubject.onNext(bitmap)
  }

  override fun createTheme(bitmap: Bitmap) = Single.create<Theme> { emitter ->
    require(Looper.myLooper() != Looper.getMainLooper()) { "Theme creation is not allowed on the main thread!" }

    // Calculate hash for this bitmap
    val hashCode = bitmap.bitmapHash()

    val cachedTheme = themeCache[hashCode]

    if (cachedTheme != null) {
      emitter.onSuccess(cachedTheme)
    } else {
      val mainPalette = Palette.from(bitmap)
        .clearFilters()
        //.addFilter(mPaletteFiler)
        .generate()

      val theme = createTheme(mainPalette)

      val statusBarPalette = Palette.from(bitmap)
        .clearFilters()
        .setRegion(0, 0, bitmap.width, 48)    // approx.
        .generate()

      theme.statusBarColor = statusBarPalette.swatches.maxBy { it.population }?.rgb
        ?: theme.primaryBackgroundColor

      themeCache.put(hashCode, theme)

      emitter.onSuccess(theme)
    }
  }.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())

  private fun createTheme(sourcePalette: Palette): Theme {

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
      val hasEnoughContrast =
        ColorUtils.calculateContrast(it.rgb, backgroundColor) > MINIMUM_CONTRAST_RATIO

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

    val redDistance = (Color.red(color2) - Color.red(color1).toDouble()).pow(2.0)
    val greenDistance = (Color.green(color2) - Color.green(color1).toDouble()).pow(2.0)
    val blueDistance = (Color.blue(color2) - Color.blue(color1).toDouble()).pow(2.0)

    return sqrt(redDistance + greenDistance + blueDistance)
  }

  companion object {
    private const val LOG_TAG = "ThemeService"

    private const val MINIMUM_CONTRAST_RATIO = 4

    private const val LAST_USED_THEME = "lastUsedTheme"

  }

}

