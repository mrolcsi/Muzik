package hu.mrolcsi.muzik.common

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.widget.SectionIndexer
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.extensions.toKeyString
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.dateAdded
import java.util.*
import kotlin.properties.Delegates

abstract class MediaItemListAdapter<VH : RecyclerView.ViewHolder>(private val context: Context) :
  ListAdapter<MediaBrowserCompat.MediaItem, VH>(DiffCallbackRepository.mediaItemCallback),
  SectionIndexer {

  @SortingMode var sorting: Int by Delegates.observable(SortingMode.SORT_BY_TITLE) { _, old, new ->
    if (old != new) {
      // Update sections
      for (i in 0 until itemCount) {
        sectionsCache += getSectionForItem(getItem(i), new)
      }
    }
  }

  private val sectionsCache = mutableSetOf<String>()

  override fun getSections(): Array<String> = sectionsCache.toTypedArray()

  override fun getSectionForPosition(position: Int): Int {
    val item = getItem(Math.min(position, itemCount - 1))
    return sections.indexOf(getSectionForItem(item))
  }

  override fun getPositionForSection(sectionIndex: Int): Int {
    for (i in 0 until itemCount) {
      if (getSectionForItem(getItem(i)) == sections[sectionIndex]) {
        return i
      }
    }
    return 0
  }

  private fun getSectionForItem(
    item: MediaBrowserCompat.MediaItem,
    @SortingMode sorting: Int = this.sorting
  ): String {
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

  override fun submitList(list: List<MediaBrowserCompat.MediaItem>?) {
    super.submitList(list)

    // Update sections (async?)
    list?.forEach {
      sectionsCache += getSectionForItem(it)
    }
  }

  companion object {
    private const val WEEK_IN_MILLISECONDS = 7 * 24 * 60 * 60 * 1000L
    private const val MONTH_IN_MILLISECONDS = 30 * 24 * 60 * 60 * 1000L
  }
}