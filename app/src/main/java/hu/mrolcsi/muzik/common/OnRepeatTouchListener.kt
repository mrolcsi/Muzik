package hu.mrolcsi.muzik.common

import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button).
 * It cyclically runs a onRepeat, emulating keyboard-like behaviour. First
 * click is fired immediately, next one after the initialInterval, and subsequent
 * ones after the normalInterval.
 *
 *
 * Interval is scheduled after the onClick completes, so it has to run fast.
 * If it runs slow, it does not generate skipped onClicks. Can be rewritten to
 * achieve this.
 */
class OnRepeatTouchListener
/**
 * @param initialInterval The interval after first click event.
 * @param normalInterval The interval after second and subsequent click events.
 * @param onRepeat The listener, that will be called periodically.
 * @param onDown An optional listener, that occurs on the first click.
 * @param onUp An optional listener, that occurs when the view is no longer being held.
 */
  (
  private val initialInterval: Int, private val normalInterval: Int,
  private val onRepeat: ((View) -> Unit)?,
  private val onDown: ((View) -> Unit)? = null,
  private val onUp: ((View) -> Unit)? = null
) : OnTouchListener {

  private val handler = Handler()
  private var touchedView: View? = null

  private val handlerRunnable = object : Runnable {

    var first = true

    override fun run() {
      if (touchedView?.isEnabled == true) {
        touchedView?.let {
          handler.postDelayed(this, normalInterval.toLong())
          if (first) {
            onDown?.invoke(it)
            first = false
          }
          onRepeat?.invoke(it)
        }
      } else {
        // if the view was disabled by the onRepeat, remove the callback
        handler.removeCallbacks(this)
        touchedView?.isPressed = false
        touchedView = null
        first = true
      }
    }
  }

  init {
    if (onRepeat == null) {
      throw IllegalArgumentException("null runnable")
    }
    if (initialInterval < 0 || normalInterval < 0) {
      throw IllegalArgumentException("negative interval")
    }
  }

  override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
    when (motionEvent.action) {
      MotionEvent.ACTION_DOWN -> {
        handler.removeCallbacks(handlerRunnable)
        handler.postDelayed(handlerRunnable, initialInterval.toLong())
        touchedView = view
        touchedView?.isPressed = true
        return true
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        handler.removeCallbacks(handlerRunnable)
        if (handlerRunnable.first) {
          // Repeat wasn't triggered yet -> preform normal click
          view.performClick()
        } else {
          onUp?.invoke(view)
          handlerRunnable.first = true
        }
        touchedView?.isPressed = false
        touchedView = null
        return true
      }
    }

    return false
  }
}