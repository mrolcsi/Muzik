package hu.mrolcsi.muzik.service.extensions.database

import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.os.bundleOf
import hu.mrolcsi.muzik.database.entities.Song
import hu.mrolcsi.muzik.service.extensions.media.album
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.duration
import hu.mrolcsi.muzik.service.extensions.media.id

fun Song.Companion.fromDescription(description: MediaDescriptionCompat) = Song(
  description.id,
  description.mediaId!!,
  description.artist,
  description.album,
  description.title.toString(),
  description.duration
)

fun Song.toDescription(): MediaDescriptionCompat = MediaDescriptionCompat.Builder()
  .setMediaId(_data)
  .setTitle(title)
  .setSubtitle(artist)
  .setDescription(album)
  .setExtras(
    bundleOf(
      // Store everything as String
      MediaStore.Audio.AudioColumns._ID to _id.toString(),
      MediaStore.Audio.AudioColumns.DURATION to duration.toString()
    )
  ).build()