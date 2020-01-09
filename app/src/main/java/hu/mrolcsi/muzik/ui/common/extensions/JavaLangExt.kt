package hu.mrolcsi.muzik.ui.common.extensions

import android.provider.MediaStore.UNKNOWN_STRING
import java.util.*

private const val TIME_FORMAT_SHORT = "%02d:%02d"
private const val TIME_FORMAT_LONG = "%02d:%02d:%02d"

fun Int.secondsToTimeStamp(): String {
  if (this < -1) {
    return "??:??"
  }

  var remaining = this

  val hours = remaining / 60 / 60
  remaining -= hours * 60 * 60

  val minutes = remaining / 60
  remaining -= minutes * 60

  val seconds = remaining

  return if (hours > 0) {
    // Use long format
    String.format(TIME_FORMAT_LONG, hours, minutes, seconds)
  } else {
    // Use short format
    String.format(TIME_FORMAT_SHORT, minutes, seconds)
  }
}

fun Long?.millisecondsToTimeStamp(): String {
  if (this == null) {
    return "??:??"
  }

  if (this < -1) {
    return "??:??"
  }

  var remaining = this

  val hours = remaining / 1000 / 60 / 60
  remaining -= hours * 60 * 60 * 1000

  val minutes = remaining / 1000 / 60
  remaining -= minutes * 60 * 1000

  val seconds = remaining / 1000
  remaining -= seconds * 1000

  return if (hours > 0) {
    // Use long format
    String.format(TIME_FORMAT_LONG, hours, minutes, seconds)
  } else {
    // Use short format
    String.format(TIME_FORMAT_SHORT, minutes, seconds)
  }
}

fun String.toKeyString(): String {
  // Copied from MediaStore.Audio.keyFor(String)
  var key = this

  if (this == UNKNOWN_STRING) {
    return "\u0001"
  }

  key = key.trim { it <= ' ' }.toLowerCase(Locale.ROOT)
  if (key.startsWith("the ")) {
    key = key.substring(4)
  }
  if (key.startsWith("an ")) {
    key = key.substring(3)
  }
  if (key.startsWith("a ")) {
    key = key.substring(2)
  }
  if (key.endsWith(", the") || key.endsWith(",the") ||
    key.endsWith(", an") || key.endsWith(",an") ||
    key.endsWith(", a") || key.endsWith(",a")
  ) {
    key = key.substring(0, key.lastIndexOf(','))
  }
  key = key.replace("[\\[\\]()\"'.,?!]".toRegex(), "").trim { it <= ' ' }
  return key
}

fun CharSequence.toKeyString(): String = this.toString().toKeyString()

fun Int.toColorHex() = String.format("#%X", this)