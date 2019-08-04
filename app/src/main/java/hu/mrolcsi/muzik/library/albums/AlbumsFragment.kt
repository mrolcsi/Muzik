package hu.mrolcsi.muzik.library.albums

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.library.albums.details.AlbumDetailsFragmentArgs
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_albums.*
import kotlinx.android.synthetic.main.list_item_album_content.view.*

class AlbumsFragment : Fragment() {

  private lateinit var viewModel: AlbumsViewModelImpl

  private val albumsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      AlbumHolder(
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.list_item_album_vertical, parent, false)
      ).apply {
        itemView.setOnClickListener { view ->
          model?.let {
            findNavController().navigate(
              R.id.navigation_albumDetails,
              AlbumDetailsFragmentArgs(it).toBundle(),
              null,
              FragmentNavigatorExtras(view.imgCoverArt to "coverArt")
            )
          }
        }
      }
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    setHasOptionsMenu(true)

    postponeEnterTransition()

    activity?.run {
      viewModel = ViewModelProviders.of(this).get(AlbumsViewModelImpl::class.java).apply {
        albums.observe(viewLifecycleOwner, Observer { albums ->
          Log.d(LOG_TAG, "Got items from LiveData: $albums")

          albumsAdapter.submitList(albums)
        })

        sorting.observe(viewLifecycleOwner, Observer {
          // Update adapter
          albumsAdapter.sorting = it
        })
      }
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, Observer {
      applyThemeAnimated(it)
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_albums, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvAlbums.setAdapter(albumsAdapter)
    rvAlbums.recyclerView.run {
      viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          viewTreeObserver.removeOnGlobalLayoutListener(this)
          startPostponedEnterTransition()
        }
      })
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_albums, menu)

    // Apply theme to items
    val color = ThemeManager.getInstance(requireContext()).currentTheme.value?.primaryForegroundColor ?: Color.WHITE
    menu.forEach {
      it.icon.setTint(color)
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    when (viewModel.sorting.value) {
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
        viewModel.sorting.value = SortingMode.SORT_BY_ARTIST
        true
      }
      R.id.menuSortByTitle -> {
        viewModel.sorting.value = SortingMode.SORT_BY_TITLE
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.connect()
  }

  override fun onStop() {
    super.onStop()
    viewModel.disconnect()
  }

  private fun applyThemeAnimated(theme: Theme) {

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme
    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    ValueAnimator.ofArgb(
      previousTheme?.secondaryBackgroundColor ?: ContextCompat.getColor(requireContext(), R.color.backgroundColor),
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

  companion object {
    private const val LOG_TAG = "AlbumsFragment"
  }
}