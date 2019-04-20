package hu.mrolcsi.muzik.common.coordinator

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat

class ScrollUpBehavior(context: Context, attrs: AttributeSet) :
  CoordinatorLayout.Behavior<View>(context, attrs) {

  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: View,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int
  ): Boolean {
    return axes == ViewCompat.SCROLL_AXIS_VERTICAL
  }

  override fun onNestedPreScroll(
    coordinatorLayout: CoordinatorLayout,
    child: View,
    target: View,
    dx: Int,
    dy: Int,
    consumed: IntArray,
    type: Int
  ) {
    super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    child.translationY = Math.min(0.0f, Math.max(-child.height.toFloat(), child.translationY - dy))
    Log.v(LOG_TAG, "translationY = ${child.translationY}")
  }

  companion object {
    private const val LOG_TAG = "ScrollUpBehavior"
  }

}