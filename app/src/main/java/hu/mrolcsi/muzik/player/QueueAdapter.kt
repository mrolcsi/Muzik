package hu.mrolcsi.muzik.player

import android.support.v4.media.session.MediaSessionCompat
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.common.DiffCallbacks
import hu.mrolcsi.muzik.common.view.MVVMListAdapter

class QueueAdapter : MVVMListAdapter<MediaSessionCompat.QueueItem, QueueItemHolder>(
  diffCallback = DiffCallbacks.queueItemCallback,
  viewHolderFactory = { parent, _ -> QueueItemHolder(parent) }
) {

//  private val mBackgroundExecutor = Executors.newSingleThreadExecutor()
//
//  override fun onBindViewHolder(holder: QueueItemHolder, position: Int) {
//    Log.v(LOG_TAG, "onBindViewHolder($holder, $position")
//
//    // Cache items surrounding position
//    val start = max(position - WINDOW_SIZE, 0)
//    val end = min(position + WINDOW_SIZE, itemCount)
//    for (i in start until end) {
//      val item = getItem(i)
//      val key = item.description.mediaId
//
//      if (mCache[key] == null) {
//        mBackgroundExecutor.submit {
//          Log.v(LOG_TAG, "Caching item: $item")
//
//          // Load item into cache
//          MediaStore.Images.Media.getBitmap(
//            holder.itemView.context.contentResolver,
//            item.description.coverArtUri
//          )?.let {
//            val theme = ThemeManager.getInstance(holder.itemView.context).createFromBitmap(it)
//            mCache.put(key, it to theme)
//          }
//        }
//      }
//    }
//
//    super.onBindViewHolder(holder, position)
//  }

  override fun getItemId(position: Int): Long {
    return getItem(position).queueId
  }

  fun getItemPositionById(id: Long): Int {
    for (i in 0 until itemCount) {
      if (getItemId(i) == id) {
        return i
      }
    }
    return RecyclerView.NO_POSITION
  }

//  companion object {
//    private const val LOG_TAG = "QueueAdapter"
//
//    private const val WINDOW_SIZE = 3
//
//    // Keep it "static" so it can be accessed from the ViewHolder as well.
//    private val mCache = LruCache<String, Pair<Bitmap, Theme>>(10)
//  }
}