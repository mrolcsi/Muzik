package hu.mrolcsi.muzik.ui.common

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

class MVVMListAdapter<ItemViewModel, ViewHolder : MVVMViewHolder<in ItemViewModel>>(
  private val itemIdSelector: (item: ItemViewModel) -> Long,
  diffCallback: DiffUtil.ItemCallback<ItemViewModel> = SimpleDiffCallback { itemIdSelector(it) },
  private val viewTypeSelector: (model: ItemViewModel) -> Int = { 0 },
  private val viewHolderFactory: ViewHolderFactory<ViewHolder>
) : ListAdapter<ItemViewModel, ViewHolder>(diffCallback), Observer<List<ItemViewModel>> {

  init {
    this.setHasStableIds(true)
  }

  override fun onChanged(items: List<ItemViewModel>?) {
    submitList(items)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    viewHolderFactory.invoke(parent, viewType)

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.model = getItem(position)
  }

  override fun getItemViewType(position: Int): Int = viewTypeSelector.invoke(getItem(position))

  override fun getItemId(position: Int): Long {
    return itemIdSelector.let {
      try {
        itemIdSelector.invoke(getItem(position))
      } catch (e: IndexOutOfBoundsException) {
        super.getItemId(position)
      }
    }
  }
}