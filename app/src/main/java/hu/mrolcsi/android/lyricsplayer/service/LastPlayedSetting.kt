package hu.mrolcsi.android.lyricsplayer.service

import android.content.Context
import android.preference.PreferenceManager
import kotlin.properties.Delegates

class LastPlayedSetting(context: Context) {

  private val mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

  var lastPlayedMedia: String? by Delegates.observable(
    mSharedPrefs.getString(
      KEY_LAST_PLAYED_MEDIA,
      null
    )
  ) { _, _, new ->
    mSharedPrefs.edit().putString(KEY_LAST_PLAYED_MEDIA, new).apply()
  }

  var lastPlayedPosition: Long by Delegates.observable(
    mSharedPrefs.getLong(
      KEY_LAST_PLAYED_POSITION,
      0
    )
  ) { _, _, new ->
    mSharedPrefs.edit().putLong(KEY_LAST_PLAYED_POSITION, new).apply()
  }

  companion object {
    private const val KEY_LAST_PLAYED_MEDIA = "LastPlayedMediaId"
    private const val KEY_LAST_PLAYED_POSITION = "LastPlayedPosition"
  }
}