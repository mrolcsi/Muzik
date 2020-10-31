package hu.mrolcsi.muzik.data.service.theme

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.util.LruCache
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.get
import androidx.core.graphics.luminance
import androidx.core.os.bundleOf
import androidx.palette.graphics.Palette
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import hu.mrolcsi.muzik.data.model.theme.Theme
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import kotlin.math.abs

class ThemeServiceImpl : ThemeService, KoinComponent {

  private val sharedPrefs: SharedPreferences by inject()
  private val gson: Gson by inject()
  private val firebase: FirebaseAnalytics by inject()

  private val themeCache = LruCache<Int, Theme>(50)

  private var lastUsedTheme: Theme?
    get() = sharedPrefs.getString(LAST_USED_THEME, null)?.let {
      gson.fromJson(it, Theme::class.java)
    }
    set(value) = sharedPrefs.edit {
      putString(LAST_USED_THEME, gson.toJson(value))
    }

  private val pendingThemeSubject: Subject<Observable<Theme>> = PublishSubject.create()

  override val currentTheme: Observable<Theme> =
    pendingThemeSubject
      .switchMap { it }
      .doOnNext { Timber.d("Theme ready: $it") }
      .replay(1)
      .apply { connect() }
      .hide()
      .startWith(lastUsedTheme ?: Theme.DEFAULT_THEME)

  override fun updateTheme(bitmap: Bitmap) {
    createTheme(bitmap)
      .subscribeOn(Schedulers.computation())
      .doOnSuccess {
        Timber.d("Updating theme: $it")
        lastUsedTheme = it
      }
      .toObservable()
      .also(pendingThemeSubject::onNext)
  }

  override fun createTheme(bitmap: Bitmap) = Single.fromCallable {
    val startTime = System.currentTimeMillis()
    require(Looper.myLooper() != Looper.getMainLooper()) { "Theme creation is not allowed on the main thread!" }

    // Calculate hash for this bitmap
    val hashCode = bitmap.bitmapHash()

    (themeCache[hashCode] ?: createThemeInternal(bitmap).also { themeCache.put(hashCode, it) }).also {
      firebase.logEvent(
        "theme_creation", bundleOf(
          "duration" to System.currentTimeMillis() - startTime
        )
      )
    }
  }.subscribeOn(Schedulers.computation())

  private fun createThemeInternal(bitmap: Bitmap): Theme {
    val sourcePalette = Palette.from(bitmap)
      .clearFilters()
      .generate()

    // Merge similar colors
    val source = sourcePalette.swatches.map { it.rgb to it.population * it.weight }.toMutableList()
    val swatches = mutableListOf<Pair<Int, Double>>()
    while (source.isNotEmpty()) {
      // Pick first color
      var pick = source.first().also { source.remove(it) }
      source.toList().forEach {
        // Check for similar colors
        if (areColorsSimilar(pick.first, it.first)) {
          // Blend picked color with current
          pick = ColorUtils.blendARGB(pick.first, it.first, 0.5f) to pick.second + it.second
          // Remove used color from the source
          source.remove(it)
        }
      }
      swatches.add(pick)
    }

    val colors = swatches
      .sortedByDescending { it.second }
      .map { it.first }
      .toMutableList()

    val primaryBackground = colors.first().also { colors.remove(it) }
    val primaryForeground = findForegroundColor(primaryBackground, colors).also { colors.remove(it) }

    return Theme(
      sourcePalette,
      primaryBackground,
      primaryForeground
    )
  }

  private fun findForegroundColor(backgroundColor: Int, colors: List<Int>): Int =
    colors
      .firstOrNull { ColorUtils.calculateContrast(it, backgroundColor) > MINIMUM_CONTRAST_RATIO }
      ?: colors.firstOrNull { ColorUtils.calculateContrast(it, backgroundColor) > MINIMUM_CONTRAST_RATIO }
      ?: colors.maxByOrNull { ColorUtils.calculateContrast(it, backgroundColor) }
      ?: run {
        if (backgroundColor.luminance < 0.5) Color.WHITE
        else Color.BLACK
      }

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

  private fun areColorsSimilar(first: Int, second: Int): Boolean =
    abs(Color.red(first) - Color.red(second)) <= SIMILARITY_THRESHOLD &&
      abs(Color.green(first) - Color.green(second)) <= SIMILARITY_THRESHOLD &&
      abs(Color.blue(first) - Color.blue(second)) <= SIMILARITY_THRESHOLD

  private val Palette.Swatch.weight
    get() = 1 - abs(0.5 - hsl[2])

  companion object {
    private const val MINIMUM_CONTRAST_RATIO = 4

    private const val LAST_USED_THEME = "lastUsedTheme"

    private const val SIMILARITY_THRESHOLD = 16
  }
}