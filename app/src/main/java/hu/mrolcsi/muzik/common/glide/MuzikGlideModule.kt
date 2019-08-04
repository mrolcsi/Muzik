package hu.mrolcsi.muzik.common.glide

import android.content.Context
import android.graphics.Bitmap
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


@GlideModule
class MuzikGlideModule : AppGlideModule() {

  // See: https://medium.com/@nuhkocaa/manage-all-your-glides-in-a-single-class-with-glidemodule-on-android-4856ee4983a1

  override fun applyOptions(context: Context, builder: GlideBuilder) {
    val factory = DrawableCrossFadeFactory.Builder(context.resources.getInteger(R.integer.preferredAnimationDuration))
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
  }

  @Deprecated("use onResourceReady and onLoadFailed instead!")
  interface SimpleRequestListener<R> : RequestListener<R> {

    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<R>?, isFirstResource: Boolean): Boolean {
      onLoadFailed()
      return false
    }

    fun onLoadFailed() {}

    override fun onResourceReady(
      resource: R,
      model: Any?,
      target: Target<R>?,
      dataSource: DataSource?,
      isFirstResource: Boolean
    ): Boolean {
      onResourceReady(resource)
      return false
    }

    fun onResourceReady(resource: R?) {}

  }

  companion object {
    private const val MEMORY_CACHE_SIZE = 1024 * 1024 * 20 // 20mb
  }
}

fun <T> GlideRequest<T>.onResourceReady(callback: (resource: T?) -> Unit): GlideRequest<T> {
  return addListener(object : MuzikGlideModule.SimpleRequestListener<T> {
    override fun onResourceReady(resource: T?) {
      callback.invoke(resource)
    }
  })
}

fun <T> GlideRequest<T>.onLoadFailed(callback: (error: GlideException?) -> Boolean) =
  addListener(object : MuzikGlideModule.SimpleRequestListener<T> {
    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<T>?, isFirstResource: Boolean): Boolean {
      return callback.invoke(e)
    }
  })