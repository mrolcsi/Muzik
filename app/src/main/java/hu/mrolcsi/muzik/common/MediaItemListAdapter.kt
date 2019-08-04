package hu.mrolcsi.muzik.common

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import com.l4digital.fastscroll.FastScroller
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.view.MVVMListAdapter
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.common.view.ViewHolderFactory
import hu.mrolcsi.muzik.extensions.toKeyString
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.dateAdded
import java.util.*

open class MediaItemListAdapter<VH : MVVMViewHolder<MediaBrowserCompat.MediaItem>>(
  private val context: Context,
  viewHolderFactory: ViewHolderFactory<VH>
) : MVVMListAdapter<MediaBrowserCompat.MediaItem, VH>(DiffCallbacks.mediaItemCallback, viewHolderFactory),
  FastScroller.SectionIndexer {

  @SortingMode var sorting: Int = SortingMode.SORT_BY_TITLE

  override fun getSectionText(position: Int): CharSequence {
    val item = getItem(position)
    return when (sorting) {
      SortingMode.SORT_BY_ARTIST -> item.description.artist?.toKeyString()?.first()?.toUpperCase().toString()
      SortingMode.SORT_BY_TITLE -> item.description.title?.toString()?.toKeyString()?.first()?.toUpperCase().toString()
      SortingMode.SORT_BY_DATE -> {
        val newThreshold = Calendar.getInstance().timeInMillis - WEEK_IN_MILLISECONDS
        val recentThreshold = Calendar.getInstance().timeInMillis - MONTH_IN_MILLISECONDS
        when {
          item.description.dateAdded > newThreshold -> context.getString(R.string.dateAdded_new)
          item.description.dateAdded > recentThreshold -> context.getString(R.string.dateAdded_recent)
          else -> context.getString(R.string.dateAdded_old)
        }
      }
      else -> throw IllegalArgumentException("unknown sorting constant.")
    }
  }

  companion object {
    private const val WEEK_IN_MILLISECONDS = 7 * 24 * 60 * 60 * 1000L
    private const val MONTH_IN_MILLISECONDS = 30 * 24 * 60 * 60 * 1000L
  }
}