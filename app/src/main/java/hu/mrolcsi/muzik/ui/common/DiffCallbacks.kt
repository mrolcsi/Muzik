package hu.mrolcsi.muzik.ui.common

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil

class SimpleDiffCallback<T>(
  private val keySelector: (T) -> Any
) : DiffUtil.ItemCallback<T>() {

  override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
    return keySelector.invoke(oldItem) == keySelector.invoke(newItem)
  }

  @SuppressLint("DiffUtilEquals")
  override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
    return oldItem == newItem
  }
}