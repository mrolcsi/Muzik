package hu.mrolcsi.muzik.common.glide

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import hu.mrolcsi.muzik.R


@GlideModule
class LPGlideModule : AppGlideModule() {

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

  companion object {
    private const val MEMORY_CACHE_SIZE = 1024 * 1024 * 20 // 20mb
  }
}