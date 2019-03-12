package hu.mrolcsi.android.lyricsplayer

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import hu.mrolcsi.android.lyricsplayer.theme.Theme

@GlideModule
class LPGlideModule : AppGlideModule() {

  // See: https://medium.com/@nuhkocaa/manage-all-your-glides-in-a-single-class-with-glidemodule-on-android-4856ee4983a1

  override fun applyOptions(context: Context, builder: GlideBuilder) {
    val factory = DrawableCrossFadeFactory.Builder(Theme.PREFERRED_ANIMATION_DURATION.toInt())
      .setCrossFadeEnabled(true)
      .build()

    builder.setDefaultTransitionOptions(
      Drawable::class.java,
      DrawableTransitionOptions.withCrossFade(factory)
    )
  }
}