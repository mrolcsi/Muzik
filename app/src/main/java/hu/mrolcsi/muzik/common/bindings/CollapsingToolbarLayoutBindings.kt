package hu.mrolcsi.muzik.common.bindings

import androidx.databinding.BindingMethod
import androidx.databinding.BindingMethods
import com.google.android.material.appbar.CollapsingToolbarLayout

@BindingMethods(
  BindingMethod(
    type = CollapsingToolbarLayout::class,
    attribute = "collapsedTitleTextColor",
    method = "setCollapsedTitleTextColor"
  ),
  BindingMethod(
    type = CollapsingToolbarLayout::class,
    attribute = "expandedTitleTextColor",
    method = "setExpandedTitleColor"
  )
)
object CollapsingToolbarLayoutBindings