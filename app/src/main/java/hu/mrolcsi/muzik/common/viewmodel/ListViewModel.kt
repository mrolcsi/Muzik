package hu.mrolcsi.muzik.common.viewmodel

import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.lifecycle.LiveData

interface ListViewModel<T> : Observable {

  @get:Bindable
  val progressVisible: Boolean

  @get:Bindable
  val listViewVisible: Boolean

  @get:Bindable
  val emptyViewVisible: Boolean

  val items: LiveData<List<T>>

  fun onSelect(item: T) {}
  fun onRefresh() {}
}