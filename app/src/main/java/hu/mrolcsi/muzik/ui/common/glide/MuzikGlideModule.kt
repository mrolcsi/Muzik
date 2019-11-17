package hu.mrolcsi.muzik.ui.common.glide

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import hu.mrolcsi.muzik.R
import io.reactivex.Single


@GlideModule
class MuzikGlideModule : AppGlideModule() {

  // See: https://medium.com/@nuhkocaa/manage-all-your-glides-in-a-single-class-with-glidemodule-on-android-4856ee4983a1

  override fun applyOptions(context: Context, builder: GlideBuilder) {
    val factory =
      DrawableCrossFadeFactory.Builder(context.resources.getInteger(R.integer.preferredAnimationDuration))
        .setCrossFadeEnabled(true)
        .build()

    builder.setDefaultTransitionOptions(
      Bitmap::class.java,
      BitmapTransitionOptions.withCrossFade(factory)
    )

    builder.setMemoryCache(LruResourceCache(MEMORY_CACHE_SIZE.toLong()))
    builder.setDiskCache(InternalCacheDiskCacheFactory(context, (MEMORY_CACHE_SIZE * 10).toLong()))

    builder.setDefaultRequestOptions(
      RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .placeholder(null)
        .error(R.drawable.placeholder_cover_art)
    )

    builder.setLogLevel(Log.ERROR)
  }

  companion object {
    private const val MEMORY_CACHE_SIZE = 1024 * 1024 * 20 // 20mb
  }
}

fun <R> GlideRequest<R>.onResourceReady(callback: (resource: R) -> Unit): GlideRequest<R> {
  return addListener(object : RequestListener<R> {
    override fun onLoadFailed(
      e: GlideException?,
      model: Any,
      target: Target<R>,
      isFirstResource: Boolean
    ): Boolean = false

    override fun onResourceReady(
      resource: R,
      model: Any,
      target: Target<R>,
      dataSource: DataSource?,
      isFirstResource: Boolean
    ): Boolean {
      callback.invoke(resource)
      return false
    }

  })
}

fun <R> GlideRequest<R>.onLoadFailed(callback: (error: GlideException?) -> Boolean) =
  addListener(object : RequestListener<R> {
    override fun onResourceReady(
      resource: R,
      model: Any?,
      target: Target<R>?,
      dataSource: DataSource?,
      isFirstResource: Boolean
    ): Boolean = false

    override fun onLoadFailed(
      e: GlideException?,
      model: Any?,
      target: Target<R>?,
      isFirstResource: Boolean
    ): Boolean {
      return callback.invoke(e)
    }
  })

fun <R> GlideRequest<R>.toSingle() = Single.create<R> { emitter ->
  this.addListener(object : RequestListener<R> {
    override fun onLoadFailed(
      e: GlideException?,
      model: Any?,
      target: Target<R>?,
      isFirstResource: Boolean
    ): Boolean {
      if (!emitter.isDisposed) emitter.onError(e!!)
      return true
    }

    override fun onResourceReady(
      resource: R,
      model: Any?,
      target: Target<R>?,
      dataSource: DataSource?,
      isFirstResource: Boolean
    ): Boolean {
      if (!emitter.isDisposed) emitter.onSuccess(resource)
      return true
    }
  }).preload()
}