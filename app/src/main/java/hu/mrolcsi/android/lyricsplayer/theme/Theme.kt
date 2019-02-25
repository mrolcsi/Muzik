package hu.mrolcsi.android.lyricsplayer.theme

import androidx.palette.graphics.Palette

data class Theme(
  var palette: Palette,

  var backgroundColor: Int,
  var foregroundColor: Int,

  var darkBackgroundColor: Int,
  var darkForegroundColor: Int,

  var darkerBackgroundColor: Int,
  var darkerForegroundColor: Int
)