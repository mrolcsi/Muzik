package hu.mrolcsi.muzik.database.playqueue.entities

import android.provider.MediaStore
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_queue")
data class PlayQueueEntry(
  @PrimaryKey @ColumnInfo(name = MediaStore.Audio.Media._ID) val _id: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.DATA) val _data: String,
  @ColumnInfo(name = "media_id") val mediaId: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.ARTIST) val artist: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.ALBUM) val album: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.TITLE) val title: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.DURATION) val duration: Long?
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PlayQueueEntry) return false

    if (_id != other._id) return false
    if (_data != other._data) return false
    if (mediaId != other.mediaId) return false
    if (artist != other.artist) return false
    if (album != other.album) return false
    if (title != other.title) return false
    if (duration != other.duration) return false

    return true
  }

  override fun hashCode(): Int {
    var result = _id.hashCode()
    result = 31 * result + _data.hashCode()
    result = 31 * result + mediaId.hashCode()
    result = 31 * result + (artist?.hashCode() ?: 0)
    result = 31 * result + (album?.hashCode() ?: 0)
    result = 31 * result + (title?.hashCode() ?: 0)
    result = 31 * result + (duration?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "PlayQueueEntry(_id=$_id, _data='$_data', mediaId=$mediaId, artist=$artist, album=$album, title=$title, duration=$duration)"
  }

  companion object

}