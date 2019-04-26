package hu.mrolcsi.muzik.common.fastscroller

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import xyz.danoz.recyclerviewfastscroller.AbsRecyclerViewFastScroller
import xyz.danoz.recyclerviewfastscroller.sectionindicator.SectionIndicator

class AutoHidingFastScrollerTouchListener(
  private val fastScroller: AbsRecyclerViewFastScroller
) : View.OnTouchListener {

  private var downTouch = false

  private val showHideAnimator = ValueAnimator.ofFloat().apply {
    interpolator = DecelerateInterpolator()
    duration = 150L
    addUpdateListener {
      fastScroller.translationX = it.animatedValue as Float
    }
  }

  val autoHideOnScrollListener: RecyclerView.OnScrollListener
    get() = object : RecyclerView.OnScrollListener() {

      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        fastScroller.onScrollListener.onScrolled(recyclerView, dx, dy)
      }

      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        when (newState) {
          RecyclerView.SCROLL_STATE_DRAGGING -> {
            startShowAnimation()
          }
          RecyclerView.SCROLL_STATE_IDLE -> {
            if (!downTouch) startHideAnimation()
          }
        }
      }
    }

  init {
    fastScroller.run {
      viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          viewTreeObserver.removeOnGlobalLayoutListener(this)
          translationX = width.toFloat()
        }
      })
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouch(v: View, event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        downTouch = true
        startShowAnimation()
      }
      MotionEvent.ACTION_UP -> {
        downTouch = false
        startHideAnimation()
      }
    }

    val sectionIndicator = fastScroller.sectionIndicator
    showOrHideIndicator(sectionIndicator, event)

    val scrollProgress = fastScroller.getScrollProgress(event)
    fastScroller.scrollTo(scrollProgress, true)
    fastScroller.moveHandleToPosition(scrollProgress)
    return true
  }

  private fun showOrHideIndicator(sectionIndicator: SectionIndicator<*>?, event: MotionEvent) {
    if (sectionIndicator == null) {
      return
    }

    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        sectionIndicator.animateAlpha(1f)
        return
      }
      MotionEvent.ACTION_UP -> sectionIndicator.animateAlpha(0f)
    }
  }

  fun startShowAnimation() {
    showHideAnimator.cancel()
    showHideAnimator.setFloatValues(fastScroller.translationX, 0f)
    showHideAnimator.startDelay = 0
    showHideAnimator.start()
  }

  fun startHideAnimation() {
    showHideAnimator.cancel()
    showHideAnimator.setFloatValues(fastScroller.translationX, fastScroller.width.toFloat())
    showHideAnimator.startDelay = 1000
    showHideAnimator.start()
  }
}