package hu.mrolcsi.muzik.library.albums

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.theme.Theme
import kotlinx.android.synthetic.main.fragment_albums.*
import kotlinx.android.synthetic.main.list_item_album_content.*
import javax.inject.Inject

class AlbumsFragment : DaggerFragment() {

  @Inject lateinit var viewModel: AlbumsViewModel

  private val albumsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      AlbumHolder(
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.list_item_album_vertical, parent, false)
      ).apply {
        itemView.setOnClickListener { _ ->
          model?.let {
            viewModel.onAlbumClicked(it, imgCoverArt)
          }
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setHasOptionsMenu(true)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {

      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      items.observe(viewLifecycleOwner, Observer { albums ->
        albumsAdapter.submitList(albums)
      })
    }

    viewModel.currentTheme.observe(
      viewLifecycleOwner,
      Observer {
      applyThemeAnimated(it)
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_albums, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvAlbums.setAdapter(albumsAdapter)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_albums, menu)

    // Apply theme to items
    val color = viewModel.currentTheme.value?.primaryForegroundColor ?: Color.WHITE
    menu.forEach {
      it.icon.setTint(color)
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    when (viewModel.sortingMode) {
      SortingMode.SORT_BY_ARTIST -> menu.findItem(R.id.menuSortByArtist).isChecked = true
      SortingMode.SORT_BY_TITLE -> menu.findItem(R.id.menuSortByTitle).isChecked = true
      else -> {
        // nothing
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    item.isChecked = true
    return when (item.itemId) {
      R.id.menuSortByArtist -> {
        viewModel.sortingMode = SortingMode.SORT_BY_ARTIST
        albumsAdapter.sorting = SortingMode.SORT_BY_ARTIST
        true
      }
      R.id.menuSortByTitle -> {
        viewModel.sortingMode = SortingMode.SORT_BY_TITLE
        albumsAdapter.sorting = SortingMode.SORT_BY_TITLE
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun applyThemeAnimated(theme: Theme) {

    val previousTheme = viewModel.previousTheme
    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    ValueAnimator.ofArgb(
      previousTheme?.secondaryBackgroundColor ?: ContextCompat.getColor(
        requireContext(),
        R.color.backgroundColor
      ),
      theme.secondaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        rvAlbums?.setBackgroundColor(color)

        rvAlbums.fastScroller.setBubbleTextColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      previousTheme?.secondaryForegroundColor ?: Color.WHITE,
      theme.secondaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        rvAlbums.fastScroller.apply {
          setTrackColor(color)
          setHandleColor(color)
          setBubbleColor(color)
        }

      }
      start()
    }
  }

}