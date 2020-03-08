package hu.mrolcsi.muzik.util

import android.support.v4.media.MediaMetadataCompat
import io.mockk.Matcher
import kotlin.test.assertEquals

class MediaMetadataCompatMatcher(private val expected: MediaMetadataCompat?) : Matcher<MediaMetadataCompat> {

  override fun match(arg: MediaMetadataCompat?): Boolean {
    if (expected == null && arg == null) return true
    if (expected == null && arg != null) return false
    if (expected != null && arg == null) return false

    val expectedBundle = expected?.bundle
    val actualBundle = arg?.bundle

    assertEquals(
      expectedBundle?.keySet()?.sorted()?.map { expectedBundle.get(it) }.toString(),
      actualBundle?.keySet()?.sorted()?.map { actualBundle.get(it) }.toString()
    )

    return true
  }

}