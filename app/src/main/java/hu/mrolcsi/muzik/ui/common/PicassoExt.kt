package hu.mrolcsi.muzik.ui.common

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Target
import io.reactivex.Single
import timber.log.Timber

fun RequestCreator.into(
  onPrepareLoad: (placeHolderDrawable: Drawable?) -> Unit = {},
  onBitmapFailed: (e: Exception, errorDrawable: Drawable?) -> Unit = { e, _ -> Timber.e(e) },
  onBitmapLoaded: (bitmap: Bitmap, from: Picasso.LoadedFrom) -> Unit
) = into(object : Target {
  override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
    onBitmapLoaded(bitmap, from)
  }

  override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
    onBitmapFailed(e, errorDrawable)
  }

  override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    onPrepareLoad(placeHolderDrawable)
  }
})

fun RequestCreator.toSingle(): Single<Bitmap> = Single.create { emitter ->
  val target = object : Target {
    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
      emitter.onSuccess(bitmap)
    }

    override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
      emitter.onError(e)
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    }
  }

  into(target)
  emitter.setCancellable { Picasso.get().cancelRequest(target) }
}