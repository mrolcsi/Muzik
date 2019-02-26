package hu.mrolcsi.android.lyricsplayer.extensions;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * Interface definition for a callback to be invoked when an item in this
 * RecyclerView has been clicked.
 */
public interface OnItemClickListener<T, VH extends ViewHolder> {

  /**
   * Callback method to be invoked when an item in this RecyclerView has
   * been clicked.
   * <p>
   * Implementers can call getItem(position) if they need
   * to access the data associated with the selected item.
   *
   * @param item The data item that was clicked on.
   * @param holder The ViewHolder within the Adapter that was clicked.
   * @param position The position of the view in the adapter.
   * @param id The id of the item that was clicked.
   */
  void onItemClick(
      @NonNull T item,
      @NonNull VH holder,
      int position,
      long id
  );
}
