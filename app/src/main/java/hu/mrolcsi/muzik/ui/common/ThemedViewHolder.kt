package hu.mrolcsi.muzik.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.data.model.theme.Theme

class ThemedViewHolder<T>(
  parent: ViewGroup,
  @LayoutRes layoutId: Int,
  viewLifecycleOwner: LifecycleOwner,
  theme: LiveData<Theme>,
  bindingComponent: DataBindingComponent? = null,
  binding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
    LayoutInflater.from(parent.context),
    layoutId,
    parent,
    false,
    bindingComponent
  ).apply {
    lifecycleOwner = viewLifecycleOwner
    setVariable(BR.theme, theme)
  },
  onModelChange: (ViewDataBinding.(T) -> Unit)? = null,
  onItemClickListener: ((item: T, holder: RecyclerView.ViewHolder) -> Unit)? = null
) : BoundMVVMViewHolder<T>(parent, layoutId, bindingComponent, binding, onModelChange, onItemClickListener)