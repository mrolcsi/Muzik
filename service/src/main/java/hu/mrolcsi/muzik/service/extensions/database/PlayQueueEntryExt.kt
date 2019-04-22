package hu.mrolcsi.muzik.service.extensions.database

import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.os.bundleOf
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry
import hu.mrolcsi.muzik.service.extensions.media.album
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.duration
import hu.mrolcsi.muzik.service.extensions.media.id

fun PlayQueueEntry.Companion.fromDescription(queuePosition: Int, description: MediaDescriptionCompat) = PlayQueueEntry(
  queuePosition.toLong(),
  description.mediaId!!,
  description.id,
  description.artist,
  description.album,
  description.title.toString(),
  description.duration
)

fun PlayQueueEntry.toDescription(): MediaDescriptionCompat = MediaDescriptionCompat.Builder()
  .setMediaId(_data)
  .setTitle(title)
  .setSubtitle(artist)
  .setDescription(album)
  .setExtras(
    bundleOf(
      // Store everything as String
      MediaStore.Audio.AudioColumns._ID to mediaId.toString(),
      MediaStore.Audio.AudioColumns.DURATION to duration.toString()
    )
  ).build()