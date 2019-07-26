package hu.mrolcsi.muzik.service.exoplayer

import android.os.Handler
import java.util.concurrent.atomic.AtomicBoolean

class ProgressUpdater(
  private val updateFrequency: Long = DEFAULT_UPDATE_FREQUENCY,
  private val updateRunnable: () -> Unit
) {

  private val mUpdaterEnabled = AtomicBoolean(false)
  private val mUpdateHandler = Handler()

  private val mInnerRunnable = object : Runnable {
    override fun run() {
      updateRunnable.invoke()

      if (isEnabled) mUpdateHandler.postDelayed(this, updateFrequency)
    }
  }

  fun startUpdater() {
    if (!mUpdaterEnabled.getAndSet(true)) mUpdateHandler.post(mInnerRunnable)
  }

  fun stopUpdater() {
    mUpdaterEnabled.set(false)
  }

  val isEnabled get() = mUpdaterEnabled.get()

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "ProgressUpdater"

    private const val DEFAULT_UPDATE_FREQUENCY: Long = 500 // ms
  }
}