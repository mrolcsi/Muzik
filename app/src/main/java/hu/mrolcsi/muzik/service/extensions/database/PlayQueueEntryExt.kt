package hu.mrolcsi.muzik.service.extensions.database

import android.support.v4.media.MediaDescriptionCompat
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry
import hu.mrolcsi.muzik.service.extensions.media.album
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.duration

fun PlayQueueEntry.Companion.fromDescription(queuePosition: Int, description: MediaDescriptionCompat) = PlayQueueEntry(
  queuePosition.toLong(),
  description.mediaId!!,
  description.artist,
  description.album,
  description.title.toString(),
  description.duration
)