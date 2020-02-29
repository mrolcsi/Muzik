package hu.mrolcsi.muzik.ui.base

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.view.children
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener

/**
 * A subclass of [TabLayout] that adds an [OnTabSelectedListener] during initialization,
 * which sets the style of selected tabs bold, and unselected tabs normal.
 *
 * To make sure the listener works as intended, set a [tabTextAppearance] with normal [android:textStyle].
 */
class BoldTabLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet?,
  defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr) {

  init {
    addOnTabSelectedListener(object : OnTabSelectedListener {
      override fun onTabReselected(tab: Tab) {}

      override fun onTabUnselected(tab: Tab) {
        tab.textView.setTypeface(null, Typeface.NORMAL)
      }

      override fun onTabSelected(tab: Tab) {
        tab.textView.setTypeface(null, Typeface.BOLD)
      }
    })
  }

  private val Tab.textView
    get() = (view.children.first { it is TextView } as TextView)

}