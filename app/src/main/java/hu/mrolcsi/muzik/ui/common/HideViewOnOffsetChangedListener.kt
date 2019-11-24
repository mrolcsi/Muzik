package hu.mrolcsi.muzik.ui.common

import android.view.View
import com.google.android.material.appbar.AppBarLayout

class HideViewOnOffsetChangedListener(
  private val viewToHide: View
) : AppBarLayout.OnOffsetChangedListener {

  override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
    val backdropHeight = appBarLayout.height - appBarLayout.minimumHeight
    val ratio = -verticalOffset / backdropHeight.toFloat()

    viewToHide.alpha = ratio
  }
}