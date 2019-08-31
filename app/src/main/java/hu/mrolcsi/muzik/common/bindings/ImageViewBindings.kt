package hu.mrolcsi.muzik.common.bindings

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp

object ImageViewBindings {

  @JvmStatic
  @BindingAdapter(value = ["srcUri", "placeholder", "error"], requireAll = false)
  fun setImageUrl(view: ImageView, url: String?, placeholder: Drawable?, error: Drawable?) {
    if (url != null) {
      GlideApp.with(view)
        .load(Uri.parse(url))
        .placeholder(placeholder)
        .error(error)
        .into(view)
    } else {
      view.setImageDrawable(placeholder)
    }
  }

  @JvmStatic
  @BindingAdapter("android:src")
  fun setImageResource(view: ImageView, @DrawableRes drawableRes: Int) {
    view.setImageResource(drawableRes)
  }

}