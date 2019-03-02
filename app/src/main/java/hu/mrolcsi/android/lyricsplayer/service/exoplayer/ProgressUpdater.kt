package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.os.Handler
import java.util.concurrent.atomic.AtomicBoolean

class ProgressUpdater(
  private val updateFrequency: Long = DEFAULT_UPDATE_FREQUENCY,
  private val updateRunnable: () -> Unit
) {

  private val mUpdaterEnabled = AtomicBoolean(false)
  private val mUpdateHandler = Handler()

  fun startUpdater() {
    if (!mUpdaterEnabled.getAndSet(true)) mUpdateHandler.post(object : Runnable {
      override fun run() {
        updateRunnable.invoke()

        if (isEnabled) mUpdateHandler.postDelayed(this, updateFrequency)
      }
    })
  }

  fun stopUpdater() {
    mUpdateHandler.removeCallbacks(updateRunnable)
  }

  var isEnabled
    get() = mUpdaterEnabled.get()
    set(value) = mUpdaterEnabled.set(value)

  companion object {
    private const val DEFAULT_UPDATE_FREQUENCY: Long = 500 // ms
  }
}