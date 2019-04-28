package hu.mrolcsi.muzik.common.fastscroller

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import hu.mrolcsi.muzik.R
import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator

class SimpleSectionIndicator
@JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SectionTitleIndicator<String>(context, attrs, defStyleAttr) {

  override fun getDefaultLayoutId(): Int = R.layout.section_indicator

  override fun setSection(item: String) {
    setTitleText(item)
  }

  override fun setProgress(progress: Float) {
    super.setProgress(progress)
    val originalY = y
    val yWithOffset = originalY - height / 2f
    val newY = Math.max(0f, yWithOffset)
    y = newY
  }

  fun setIndicatorTextSize(textSizeSp: Int) {
    findViewById<TextView>(R.id.section_indicator_text).textSize = textSizeSp.toFloat()
  }

  companion object {
    const val DEFAULT_TEXT_SIZE = 36 //sp
  }
}