package hu.mrolcsi.muzik.data.model.media

import androidx.annotation.IntDef

@IntDef(
  MediaType.MEDIA_UNKNOWN,
  MediaType.MEDIA_OTHER,
  MediaType.MEDIA_ARTIST,
  MediaType.MEDIA_ALBUM,
  MediaType.MEDIA_SONG
)
@Retention(AnnotationRetention.SOURCE)
annotation class MediaType {

  companion object {
    /**
     * The String key used in bundles.
     */
    const val MEDIA_TYPE_KEY = "android.media.metadata.KEY"

    /**
     * A MediaItem of unknown type. Usually appears when no type has been set explicitly.
     */
    const val MEDIA_UNKNOWN = -1
    /**
     * A MediaItem that holds some special information that's not originating from the MediaStore.
     */
    const val MEDIA_OTHER = 0
    /**
     * A MediaItem that contains information about an artist.
     */
    const val MEDIA_ARTIST = 1
    /**
     * A MediaItem that contains information about an album.
     */
    const val MEDIA_ALBUM = 2
    /**
     * A MediaItem that contains information about a song.
     */
    const val MEDIA_SONG = 3
  }
}