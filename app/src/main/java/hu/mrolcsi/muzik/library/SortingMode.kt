package hu.mrolcsi.muzik.library

import androidx.annotation.IntDef

@IntDef(SortingMode.SORT_BY_ARTIST, SortingMode.SORT_BY_TITLE, SortingMode.SORT_BY_DATE)
@Retention(AnnotationRetention.SOURCE)
annotation class SortingMode {

  companion object {
    const val SORT_BY_ARTIST = 1
    const val SORT_BY_TITLE = 2
    const val SORT_BY_DATE = 3
  }
}