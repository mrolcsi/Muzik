package hu.mrolcsi.android.lyricsplayer.common.pager

import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class PagerSnapHelperVerbose(
  private val recyclerView: RecyclerView,
  private val externalListener: RVPagerStateListener
) : PagerSnapHelper(), ViewTreeObserver.OnGlobalLayoutListener {

  private var lastPage = RecyclerView.NO_POSITION

  init {
    recyclerView.viewTreeObserver.addOnGlobalLayoutListener(this)
  }

  override fun onGlobalLayout() {
    val position = (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
    if (position != RecyclerView.NO_POSITION) {
      notifyNewPageIfNeeded(position)
      recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
  }

  override fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View? {
    val view = super.findSnapView(layoutManager)
    view?.let {
      notifyNewPageIfNeeded(recyclerView.getChildAdapterPosition(view))
    }
    return view
  }

  override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager?, velocityX: Int, velocityY: Int): Int {
    val position = super.findTargetSnapPosition(layoutManager, velocityX, velocityY)

    recyclerView.adapter?.let {
      if (position < it.itemCount) { // Making up for a "bug" in the original snap-helper.
        notifyNewPageIfNeeded(position)
      }
    }

    return position
  }

  private fun notifyNewPageIfNeeded(page: Int) {
    if (page != lastPage) {
      this.externalListener.onPageSelected(page)
      lastPage = page
    }
  }
}
