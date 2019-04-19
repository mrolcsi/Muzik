package hu.mrolcsi.muzik.database.entities

import android.provider.MediaStore
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

open class Song(
  @PrimaryKey @ColumnInfo(name = MediaStore.Audio.Media._ID) var _id: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.DATA) var _data: String,
  @ColumnInfo(name = MediaStore.Audio.Media.ARTIST) var artist: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.ALBUM) var album: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.TITLE) var title: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.DURATION) var duration: Long?
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Song) return false

    if (_id != other._id) return false
    if (_data != other._data) return false
    if (artist != other.artist) return false
    if (album != other.album) return false
    if (title != other.title) return false
    if (duration != other.duration) return false

    return true
  }

  override fun hashCode(): Int {
    var result = _id.hashCode()
    result = 31 * result + _data.hashCode()
    result = 31 * result + (artist?.hashCode() ?: 0)
    result = 31 * result + (album?.hashCode() ?: 0)
    result = 31 * result + (title?.hashCode() ?: 0)
    result = 31 * result + (duration?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "Song(_id=$_id, _data='$_data')"
  }

  companion object
}