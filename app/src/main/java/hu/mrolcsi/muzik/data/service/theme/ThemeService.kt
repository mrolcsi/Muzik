package hu.mrolcsi.muzik.data.service.theme

import android.graphics.Bitmap
import hu.mrolcsi.muzik.data.model.theme.Theme
import io.reactivex.Observable
import io.reactivex.Single

interface ThemeService {

  val currentTheme: Observable<Theme>

  fun createTheme(bitmap: Bitmap): Single<Theme>
  fun updateTheme(bitmap: Bitmap)
}