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
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.fastscroller.AutoHidingFastScrollerTouchListener
import hu.mrolcsi.muzik.library.SessionViewModel
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_albums.*

class AlbumsFragment : Fragment() {

  private lateinit var mAlbumsModel: AlbumsViewModel

  private val mAlbumsAdapter by lazy { AlbumsAdapter(requireContext()) }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    setHasOptionsMenu(true)

    postponeEnterTransition()

    activity?.run {
      mAlbumsModel = ViewModelProviders.of(this).get(AlbumsViewModel::class.java).apply {
        albums.observe(viewLifecycleOwner, Observer { albums ->
          Log.d(LOG_TAG, "Got items from LiveData: $albums")

          mAlbumsAdapter.submitList(albums)
          rvAlbums.scrollToPosition(0)
        })

        sorting.observe(viewLifecycleOwner, Observer {
          // Update adapter
          mAlbumsAdapter.sorting = it
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
    rvAlbums.run {
      rvAlbums.adapter = mAlbumsAdapter

      viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          viewTreeObserver.removeOnGlobalLayoutListener(this)
          startPostponedEnterTransition()
        }
      })

      fastScroller.setRecyclerView(this)
      fastScroller.setOnTouchListener(AutoHidingFastScrollerTouchListener(fastScroller).also {
        addOnScrollListener(it.autoHideOnScrollListener)
      })

      fastScroller.sectionIndicator = sectionIndicator
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

    when (mAlbumsModel.sorting.value) {
      SessionViewModel.Sorting.BY_ARTIST -> menu.findItem(R.id.menuSortByArtist).isChecked = true
      SessionViewModel.Sorting.BY_TITLE -> menu.findItem(R.id.menuSortByTitle).isChecked = true
      else -> {
        // nothing
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    item.isChecked = true
    return when (item.itemId) {
      R.id.menuSortByArtist -> {
        mAlbumsModel.sorting.value = SessionViewModel.Sorting.BY_ARTIST
        true
      }
      R.id.menuSortByTitle -> {
        mAlbumsModel.sorting.value = SessionViewModel.Sorting.BY_TITLE
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun applyThemeAnimated(theme: Theme) {

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme
    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    ValueAnimator.ofArgb(
      previousTheme?.tertiaryBackgroundColor ?: ContextCompat.getColor(requireContext(), R.color.backgroundColor),
      theme.tertiaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        rvAlbums?.setBackgroundColor(color)

        sectionIndicator.setIndicatorTextColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      previousTheme?.tertiaryForegroundColor ?: Color.WHITE,
      theme.tertiaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        // Apply colors to FastScroller
        fastScroller.setBarColor(color)
        fastScroller.setHandleBackground(requireContext().getDrawable(R.drawable.fast_scroller_handle_rounded)?.apply {
          setTint(color)
        })

        sectionIndicator.setIndicatorBackgroundColor(color)
      }
      start()
    }
  }

  companion object {
    private const val LOG_TAG = "AlbumsFragment"
  }
}