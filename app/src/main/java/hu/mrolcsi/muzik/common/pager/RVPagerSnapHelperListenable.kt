package hu.mrolcsi.muzik.common.pager

import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class VisiblePageState(
  var index: Int,
  var view: View,
  @Px var viewCenterX: Int,
  @Px var distanceToSettledPixels: Int,
  var distanceToSettled: Float
)

interface RVPagerStateListener {
  fun onPageScroll(pagesState: List<VisiblePageState>) {}
  fun onScrollStateChanged(@ScrollState state: Int) {}
  fun onPageSelected(index: Int) {}
}

open class RVPagerSnapHelperListenable(private val maxPages: Int = 3) {
  fun attachToRecyclerView(recyclerView: RecyclerView, listener: RVPagerStateListener): PagerSnapHelperVerbose {
    assertRecyclerViewSetup(recyclerView)
    setUpScrollListener(recyclerView, listener)
    return setUpSnapHelper(recyclerView, listener)
  }

  private fun setUpScrollListener(recyclerView: RecyclerView, listener: RVPagerStateListener) =
    PagerSnapScrollListener(recyclerView, listener, maxPages)

  private fun setUpSnapHelper(recyclerView: RecyclerView, listener: RVPagerStateListener) =
    PagerSnapHelperVerbose(recyclerView, listener).apply {
      attachToRecyclerView(recyclerView)
    }

  private fun assertRecyclerViewSetup(recyclerView: RecyclerView) {
    if (recyclerView.layoutManager !is LinearLayoutManager) {
      throw IllegalArgumentException("RVPagerSnapHelperListenable can only work with a linear layout manager")
    }

    if ((recyclerView.layoutManager as LinearLayoutManager).orientation != LinearLayoutManager.HORIZONTAL) {
      throw IllegalArgumentException("RVPagerSnapHelperListenable can only work with a horizontal orientation")
    }
  }
}
