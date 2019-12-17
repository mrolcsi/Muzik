package hu.mrolcsi.muzik.ui.common

import android.graphics.drawable.Drawable
import androidx.viewpager.widget.PagerAdapter
import com.google.android.material.tabs.TabLayout

interface IconPagerAdapter {
  fun getIconDrawable(position: Int): Drawable?
}

fun <T> TabLayout.setupIcons(adapter: T) where T : PagerAdapter, T : IconPagerAdapter {
  for (i in 0 until adapter.count) {
    getTabAt(i)?.icon = adapter.getIconDrawable(i)
  }
}