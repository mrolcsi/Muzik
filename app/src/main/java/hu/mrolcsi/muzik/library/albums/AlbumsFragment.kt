package hu.mrolcsi.muzik.library.albums

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_albums.*

class AlbumsFragment : Fragment() {

  private lateinit var mAlbumsModel: AlbumsViewModel

  private var mAlbumsAdapter: AlbumsAdapter = AlbumsAdapter()

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    postponeEnterTransition()

    activity?.run {
      mAlbumsModel = ViewModelProviders.of(this).get(AlbumsViewModel::class.java).apply {
        albums.observe(viewLifecycleOwner, Observer { albums ->
          Log.d(LOG_TAG, "Got items from LiveData: $albums")
          mAlbumsAdapter.submitList(albums)
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
    rvBrowser.adapter = mAlbumsAdapter

    rvBrowser.viewTreeObserver.run {
      addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          removeOnGlobalLayoutListener(this)
          startPostponedEnterTransition()
        }
      })
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

        rvBrowser?.setBackgroundColor(color)
      }
      start()
    }
  }

  companion object {
    private const val LOG_TAG = "AlbumsFragment"
  }
}