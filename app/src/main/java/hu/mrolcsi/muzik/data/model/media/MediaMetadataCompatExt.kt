/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")
@file:SuppressLint("WrongConstant")

package hu.mrolcsi.muzik.data.model.media

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import java.util.*

//region -- GETTERS --

/**
 * Useful extensions for [MediaMetadataCompat].
 */

inline val MediaMetadataCompat.id: Long
  get() = getString(MediaStore.Audio.Media._ID)?.toLong()
    ?: description.id

inline val MediaMetadataCompat.mediaId: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    ?: getString(MediaStore.Audio.Media.DATA)
    ?: description.mediaId

inline val MediaMetadataCompat.title: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_TITLE)
    ?: getString(MediaStore.Audio.Media.TITLE)
    ?: description.title?.toString()

inline val MediaMetadataCompat.artist: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
    ?: getString(MediaStore.Audio.Media.ARTIST)
    ?: description.artist

inline val MediaMetadataCompat.duration: Long
  get() = Collections.max(
    listOf(
      getLong(MediaMetadataCompat.METADATA_KEY_DURATION),
      getString(MediaStore.Audio.Media.DURATION)?.toLong() ?: -1,
      description.duration
    )
  )

inline val MediaMetadataCompat.album: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
    ?: getString(MediaStore.Audio.Media.ALBUM)
    ?: description.album

inline val MediaMetadataCompat.author: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_AUTHOR)

inline val MediaMetadataCompat.writer: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_WRITER)

inline val MediaMetadataCompat.composer: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_COMPOSER)
    ?: getString(MediaStore.Audio.Media.COMPOSER)
    ?: description.composer

inline val MediaMetadataCompat.compilation: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_COMPILATION)

inline val MediaMetadataCompat.date: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_DATE)

inline val MediaMetadataCompat.year: Long
  get() = Collections.max(
    listOf(
      getLong(MediaMetadataCompat.METADATA_KEY_YEAR),
      getString(MediaStore.Audio.Media.YEAR)?.toLong() ?: -1L,
      description.year
    )
  )

inline val MediaMetadataCompat.genre: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_GENRE)

inline val MediaMetadataCompat.trackNumber: Long
  get() = Collections.max(
    listOf(
      getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER).rem(1000),
      (getString(MediaStore.Audio.Media.TRACK)?.toLong() ?: -1).rem(1000),
      description.trackNumber.rem(1000)
    )
  )

inline val MediaMetadataCompat.trackCount
  get() = getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS)

inline val MediaMetadataCompat.discNumber
  get() = trackNumber / 1000

inline val MediaMetadataCompat.albumArtist: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)
    ?: getString("album_artist")
    ?: description.albumArtist

inline val MediaMetadataCompat.albumArt: Bitmap?
  get() = getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)

inline val MediaMetadataCompat.albumArtUri: Uri?
  get() = this.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)?.let { Uri.parse(it) }
    ?: Uri.parse("content://media/external/audio/media/$id/albumart")

inline val MediaMetadataCompat.userRating: RatingCompat?
  get() = getRating(MediaMetadataCompat.METADATA_KEY_USER_RATING)

inline val MediaMetadataCompat.rating: RatingCompat?
  get() = getRating(MediaMetadataCompat.METADATA_KEY_RATING)

inline val MediaMetadataCompat.displayTitle: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)

inline val MediaMetadataCompat.displaySubtitle: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)

inline val MediaMetadataCompat.displayDescription: String?
  get() = getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION)

inline val MediaMetadataCompat.displayIcon: Bitmap?
  get() = getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON)

inline val MediaMetadataCompat.displayIconUri: Uri?
  get() = getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI)?.let { Uri.parse(it) }

inline val MediaMetadataCompat.mediaUri: Uri?
  get() = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)?.let { Uri.parse(it) }

inline val MediaMetadataCompat.downloadStatus
  get() = getLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS)

//endregion

//region -- SETTERS --

/**
 * Useful extensions for [MediaMetadataCompat.Builder].
 */

// These do not have getters, so create a message for the error.
const val NO_GET = "Property does not have a 'get'"

inline var MediaMetadataCompat.Builder.id: Long
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaStore.Audio.Media._ID, value.toString())
  }

inline var MediaMetadataCompat.Builder.mediaId: String
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, value)
  }

inline var MediaMetadataCompat.Builder.title: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_TITLE, value)
  }

inline var MediaMetadataCompat.Builder.artist: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_ARTIST, value)
  }

inline var MediaMetadataCompat.Builder.album: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_ALBUM, value)
  }

inline var MediaMetadataCompat.Builder.duration: Long
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putLong(MediaMetadataCompat.METADATA_KEY_DURATION, value)
  }

inline var MediaMetadataCompat.Builder.genre: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_GENRE, value)
  }

inline var MediaMetadataCompat.Builder.mediaUri: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, value)
  }

inline var MediaMetadataCompat.Builder.albumArtUri: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, value)
  }

inline var MediaMetadataCompat.Builder.albumArt: Bitmap?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, value)
  }

inline var MediaMetadataCompat.Builder.trackNumber: Long
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, value)
  }

inline var MediaMetadataCompat.Builder.trackCount: Long
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, value)
  }

inline var MediaMetadataCompat.Builder.displayTitle: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, value)
  }

inline var MediaMetadataCompat.Builder.displaySubtitle: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, value)
  }

inline var MediaMetadataCompat.Builder.displayDescription: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, value)
  }

inline var MediaMetadataCompat.Builder.displayIconUri: String?
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, value)
  }

inline var MediaMetadataCompat.Builder.downloadStatus: Long
  @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
  get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
  set(value) {
    putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, value)
  }

//endregion

/**
 * Custom property for retrieving a [MediaDescriptionCompat] which also includes
 * all of the keys from the [MediaMetadataCompat] object in its extras.
 *
 * These keys are used by the ExoPlayer MediaSession extension when announcing metadata changes.
 */
inline val MediaMetadataCompat.fullDescription: MediaDescriptionCompat
  get() =
    description.also {
      it.extras?.putAll(bundle)
    }

/**
 * Extension method for building an [ProgressiveMediaSource] from a [MediaMetadataCompat] object.
 *
 * For convenience, place the [MediaDescriptionCompat] into the tag so it can be retrieved later.
 */
fun MediaMetadataCompat.toMediaSource(dataSourceFactory: DataSource.Factory): MediaSource =
  ProgressiveMediaSource.Factory(dataSourceFactory)
    .setTag(fullDescription)
    .createMediaSource(mediaUri)

/**
 * Extension method for building a [ConcatenatingMediaSource] given a [List]
 * of [MediaMetadataCompat] objects.
 */
fun List<MediaMetadataCompat>.toMediaSource(
  dataSourceFactory: DataSource.Factory
): ConcatenatingMediaSource {

  val concatenatingMediaSource = ConcatenatingMediaSource()
  forEach {
    concatenatingMediaSource.addMediaSource(it.toMediaSource(dataSourceFactory))
  }
  return concatenatingMediaSource
}