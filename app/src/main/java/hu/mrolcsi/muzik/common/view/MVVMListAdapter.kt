package hu.mrolcsi.muzik.common.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
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
  private val itemIdSelector: ((item: ItemViewModel) -> Long)? = null,
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

  override fun getItemId(position: Int): Long {
    return itemIdSelector?.let {
      try {
        itemIdSelector.invoke(getItem(position))
      } catch (e: IndexOutOfBoundsException) {
        super.getItemId(position)
      }
    } ?: super.getItemId(position)
  }

  fun getItemPositionById(id: Long): Int {
    for (i in 0 until itemCount) {
      if (getItemId(i) == id) {
        return i
      }
    }
    return RecyclerView.NO_POSITION
  }
}