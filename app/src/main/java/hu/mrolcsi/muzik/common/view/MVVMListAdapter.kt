package hu.mrolcsi.muzik.common.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

typealias ViewHolderFactory<VH> = (parent: ViewGroup, viewType: Int) -> VH

abstract class MVVMViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

  constructor(@LayoutRes layoutRes: Int, parent: ViewGroup) : this(
    LayoutInflater.from(parent.context)
      .inflate(layoutRes, parent, false)
  )

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

fun <ItemViewModel, ViewHolder : MVVMViewHolder<in ItemViewModel>> RecyclerView.setup(
  lifecycleOwner: LifecycleOwner,
  items: LiveData<List<ItemViewModel>>,
  diffCallback: DiffUtil.ItemCallback<ItemViewModel>,
  viewHolderFactory: ViewHolderFactory<ViewHolder>
) {
  adapter = MVVMListAdapter(diffCallback, viewHolderFactory).apply {
    items.observe(lifecycleOwner, this)
  }
}