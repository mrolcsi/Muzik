package hu.mrolcsi.muzik.extensions

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.navigation.NavDirections

data class ParcelableNavDirections(
  @IdRes private val _actionId: Int,
  private val _arguments: Bundle
) : NavDirections, Parcelable {

  constructor(directions: NavDirections) : this(directions.actionId, directions.arguments)

  override fun getActionId(): Int = _actionId

  override fun getArguments(): Bundle = _arguments

  //region -- PARCELABLE --

  constructor(parcel: Parcel) : this(
    parcel.readInt(),
    parcel.readBundle(Bundle::class.java.classLoader)!!
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(_actionId)
    parcel.writeBundle(_arguments)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<ParcelableNavDirections> {
    override fun createFromParcel(parcel: Parcel): ParcelableNavDirections {
      return ParcelableNavDirections(parcel)
    }

    override fun newArray(size: Int): Array<ParcelableNavDirections?> {
      return arrayOfNulls(size)
    }
  }

  //endregion
}