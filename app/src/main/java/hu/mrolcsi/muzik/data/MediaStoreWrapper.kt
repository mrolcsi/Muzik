package hu.mrolcsi.muzik.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Utility class to mask MediaStore static methods as class methods.
 */
class MediaStoreWrapper(private val context: Context) {

  @WorkerThread
  @Throws(FileNotFoundException::class, IOException::class)
  fun getBitmap(url: Uri): Bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, url)

}