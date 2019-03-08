package hu.mrolcsi.android.lyricsplayer

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class LPGlideModule : AppGlideModule() {

  // See: https://medium.com/@nuhkocaa/manage-all-your-glides-in-a-single-class-with-glidemodule-on-android-4856ee4983a1

  override fun applyOptions(context: Context, builder: GlideBuilder) {
    super.applyOptions(context, builder)
    builder.apply {
      RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
    }
  }
}