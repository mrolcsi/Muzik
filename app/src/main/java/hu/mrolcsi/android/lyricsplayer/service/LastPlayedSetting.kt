package hu.mrolcsi.android.lyricsplayer.service

import android.content.Context
import android.preference.PreferenceManager
import kotlin.properties.Delegates

class LastPlayedSetting(context: Context) {

  private val mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

  var lastPlayedQueue: Set<String>
    get() = mSharedPrefs.getStringSet(KEY_LAST_PLAYED_QUEUE, emptySet())
      ?.sortedWith(mSongComparator)
      ?.map {
        it.substring(
          it.indexOf(SEPARATOR) + 1,
          it.length
        )
      }
      ?.toSet()
      ?: emptySet()
    set(value) = mSharedPrefs.edit().putStringSet(
      KEY_LAST_PLAYED_QUEUE,
      value.mapIndexed { index, item ->
        "$index$SEPARATOR$item"
      }.toSet()
    ).apply()

//  var lastPlayedQueue: Set<String> by Delegates.observable(
//    mSharedPrefs.getStringSet(KEY_LAST_PLAYED_QUEUE, emptySet())
//  ) { _, _, new ->
//    Log.v(LOG_TAG, "Saving Queue = $new")
//    mSharedPrefs.edit().putStringSet(KEY_LAST_PLAYED_QUEUE, new).apply()
//  }

  var lastPlayedIndex: Int by Delegates.observable(
    mSharedPrefs.getInt(KEY_LAST_PLAYED_INDEX, 0)
  ) { _, _, new ->
    mSharedPrefs.edit().putInt(KEY_LAST_PLAYED_INDEX, new).apply()
  }

  var lastPlayedPosition: Long by Delegates.observable(
    mSharedPrefs.getLong(KEY_LAST_PLAYED_POSITION, 0)
  ) { _, _, new ->
    mSharedPrefs.edit().putLong(KEY_LAST_PLAYED_POSITION, new).apply()
  }

  companion object {
    private const val LOG_TAG = "LastPlayedSetting"

    private const val KEY_LAST_PLAYED_QUEUE = "LastPlayedQueue"
    private const val KEY_LAST_PLAYED_INDEX = "LastPlayedIndex"
    private const val KEY_LAST_PLAYED_POSITION = "LastPlayedPosition"

    private const val SEPARATOR = '\u0000'

    private val mSongComparator = object : Comparator<String> {
      override fun compare(o1: String?, o2: String?): Int {
        return when {
          o1 == null && o2 == null -> 0
          o1 == null -> -1
          o2 == null -> 1
          else -> {
            val i1 = o1.substring(0, o1.indexOf(SEPARATOR)).toInt()
            val i2 = o2.substring(0, o2.indexOf(SEPARATOR)).toInt()
            i1 - i2
          }
        }
      }
    }
  }
}