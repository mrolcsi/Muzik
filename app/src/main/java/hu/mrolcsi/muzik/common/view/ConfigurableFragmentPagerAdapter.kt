package hu.mrolcsi.muzik.common.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter

open class ConfigurableFragmentPagerAdapter(fm: FragmentManager) :
  FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT),
  Observer<List<Page>> {

  protected val pages: MutableList<Page> = ArrayList(4)

  override fun getItem(position: Int): Fragment = pages[position].fragment

  override fun getItemId(position: Int): Long = System.identityHashCode(getItem(position)).toLong()

  override fun getItemPosition(obj: Any): Int = pages.indexOfFirst { it.fragment === obj }
    .takeIf { it >= 0 } ?: PagerAdapter.POSITION_NONE

  override fun getPageTitle(position: Int) = pages[position].title

  override fun getCount() = pages.size

  override fun onChanged(t: List<Page>) {
    setPages(t)
    notifyDataSetChanged()
  }

  fun addPage(page: Page) {
    pages.add(page)
  }

  fun addPage(index: Int, page: Page) = pages.add(index, page)

  fun setPage(index: Int, page: Page) {
    pages[index] = page
  }

  fun clear() = pages.clear()

  fun setPages(pages: List<Page>) {
    this.pages.apply {
      clear()
      addAll(pages)
    }
  }
}

data class Page(
  val title: String,
  val fragment: Fragment
)