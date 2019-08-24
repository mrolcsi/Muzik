package hu.mrolcsi.muzik.theme

import android.graphics.Bitmap
import io.reactivex.Observable

interface ThemeService {

  val currentTheme: Observable<Theme>

  fun createTheme(bitmap: Bitmap): Theme
  fun updateTheme(bitmap: Bitmap)

}