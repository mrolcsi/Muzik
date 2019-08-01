package hu.mrolcsi.muzik.common.view

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

typealias ViewHolderFactory<VH> = (parent: ViewGroup, viewType: Int) -> VH

abstract class MVVMViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
  abstract var model: T?
}

open class MVVMListAdapter<ItemViewModel, ViewHolder : MVVMViewHolder<in ItemViewModel>>(
  diffCallback: DiffUtil.ItemCallback<ItemViewModel>,
  private val viewHolderFactory: ViewHolderFactory<ViewHolder>
) : ListAdapter<ItemViewModel, ViewHolder>(diffCallback), Observer<List<ItemViewModel>> {

  override fun onChanged(items: List<ItemViewModel>?) {
    submitList(items)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    viewHolderFactory.invoke(parent, viewType)

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.model = getItem(position)
  }
}