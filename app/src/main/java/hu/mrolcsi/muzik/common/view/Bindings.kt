package hu.mrolcsi.muzik.common.view

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp

//import androidx.databinding.BindingMethod
//import androidx.databinding.BindingMethods
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
//
//
//@BindingMethods(
//  value = [
//    BindingMethod(type = SwipeRefreshLayout::class, attribute = "android:enabled", method = "setEnabled"),
//    BindingMethod(type = SwipeRefreshLayout::class, attribute = "isRefreshing", method = "setRefreshing")
//  ]
//)
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

}
