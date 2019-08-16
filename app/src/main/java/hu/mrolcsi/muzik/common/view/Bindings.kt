package hu.mrolcsi.muzik.common.view

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import androidx.databinding.BindingConversion
import hu.mrolcsi.muzik.common.glide.GlideApp

object BindingAdapters {

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

object BindingConverters {

  @JvmStatic
  @BindingConversion
  fun booleanToVisibility(isVisible: Boolean) = if (isVisible) View.VISIBLE else View.GONE

}