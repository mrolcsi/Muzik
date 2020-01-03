package hu.mrolcsi.muzik.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import kotlin.properties.Delegates

open class BoundMVVMViewHolder<T>(
  parent: ViewGroup,
  @LayoutRes layoutId: Int,
  bindingComponent: DataBindingComponent? = null,
  binding: ViewDataBinding = DataBindingUtil.inflate(
    LayoutInflater.from(parent.context),
    layoutId,
    parent,
    false,
    bindingComponent
  ),
  onModelChange: (ViewDataBinding.(model: T) -> Unit)? = null,
  onItemClickListener: ((item: T, holder: RecyclerView.ViewHolder) -> Unit)? = null
) : MVVMViewHolder<T>(binding.root) {

  override var model: T? by Delegates.observable(null) { _, _: T?, new: T? ->
    binding.setVariable(BR.model, new)
    binding.executePendingBindings()
    if (onModelChange != null && new != null) {
      binding.onModelChange(new)
    }
  }

  init {
    itemView.setOnClickListener {
      onItemClickListener?.also { listener ->
        model?.also {
          listener.invoke(it, this)
        }
      }
    }
  }
}