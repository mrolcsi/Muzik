package hu.mrolcsi.muzik.ui.common

import androidx.recyclerview.widget.DiffUtil
import com.l4digital.fastscroll.FastScroller

class IndexedMVVMListAdapter<ItemViewModel, ViewHolder : MVVMViewHolder<in ItemViewModel>>(
  itemIdSelector: (item: ItemViewModel) -> Long,
  diffCallback: DiffUtil.ItemCallback<ItemViewModel> = SimpleDiffCallback { itemIdSelector(it) },
  viewHolderFactory: ViewHolderFactory<ViewHolder>,
  private val sectionTextSelector: (item: ItemViewModel) -> CharSequence
) : MVVMListAdapter<ItemViewModel, ViewHolder>(
  itemIdSelector,
  diffCallback,
  viewHolderFactory
), FastScroller.SectionIndexer {

  override fun getSectionText(position: Int): CharSequence = sectionTextSelector(getItem(position))
}