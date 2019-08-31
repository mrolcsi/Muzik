package hu.mrolcsi.muzik.theme

import android.graphics.Bitmap
import io.reactivex.Observable
import io.reactivex.Single

interface ThemeService {

  val currentTheme: Observable<Theme>

  fun createTheme(bitmap: Bitmap): Single<Theme>
  fun updateTheme(bitmap: Bitmap)
}