package hu.mrolcsi.muzik.player

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentPlayerBinding
import hu.mrolcsi.muzik.extensions.applyForegroundColor
import hu.mrolcsi.muzik.extensions.applyNavigationBarColor
import hu.mrolcsi.muzik.extensions.applyStatusBarColor
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.content_player.*
import kotlinx.android.synthetic.main.fragment_player.*
import javax.inject.Inject

class PlayerFragment : DaggerFragment() {

  @Inject lateinit var viewModel: PlayerViewModel

  // Prepare drawables (separate for each button)
  private val mPreviousBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }
  private val mPlayPauseBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }
  private val mNextBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }

  //region LIFECYCLE

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    activity?.run {
      viewModel.apply {
        requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
        findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)
      }

      ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, object : Observer<Theme> {

        private var initialLoad = true

        override fun onChanged(it: Theme) {
          if (initialLoad) {
            applyThemeStatic(it)
            initialLoad = false
          } else {
            applyThemeAnimated(it)
          }
        }
      })
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    FragmentPlayerBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupToolbar()
  }

  //endregion

  private fun setupToolbar() {
    playerToolbar.run {
      setupWithNavController(findNavController())

      setOnMenuItemClickListener { item ->
        when (item?.itemId) {
          R.id.menuPlaylist -> {
            drawer_layout.openDrawer(GravityCompat.END)
            true
          }
          else -> super.onOptionsItemSelected(item)
        }
      }
    }
  }

  private fun applyThemeStatic(theme: Theme) {
    Log.i(LOG_TAG, "applyThemeStatic($theme)")

    applyBackgroundColor(theme.primaryBackgroundColor)
    applyForegroundColor(theme.primaryForegroundColor)
  }

  private fun applyThemeAnimated(theme: Theme) {
    Log.i(LOG_TAG, "applyingThemeAnimated($theme)")

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme
    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    // StatusBar Color
    ValueAnimator.ofArgb(
      previousTheme?.statusBarColor ?: ContextCompat.getColor(requireContext(), R.color.backgroundColor),
      theme.statusBarColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        activity?.applyStatusBarColor(color)
      }
      start()
    }

    // Background Color
    ValueAnimator.ofArgb(
      previousTheme?.primaryBackgroundColor ?: ContextCompat.getColor(requireContext(), R.color.backgroundColor),
      theme.primaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        applyBackgroundColor(color)
      }
      start()
    }

    // Foreground Color
    ValueAnimator.ofArgb(
      previousTheme?.primaryForegroundColor ?: Color.WHITE,
      theme.primaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        applyForegroundColor(color)
      }
      start()
    }
  }

  private fun applyForegroundColor(color: Int) {
    // Toolbar
    playerToolbar.applyForegroundColor(color)

    // Texts
    tvElapsedTime.setTextColor(color)
    tvRemainingTime.setTextColor(color)
    tvSeekProgress.setTextColor(color)

    // SeekBar
    sbSongProgress.progressDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    sbSongProgress.thumb.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

    // Media Buttons Background
    mPreviousBackground?.setTint(color)
    mPlayPauseBackground?.setTint(color)
    mNextBackground?.setTint(color)

    // Additional Buttons
    btnShuffle.imageTintList = ColorStateList.valueOf(color)
    btnRepeat.imageTintList = ColorStateList.valueOf(color)
  }

  private fun applyBackgroundColor(color: Int) {
    // Window background
    view?.setBackgroundColor(color)

    // Navigation Bar
    activity?.window?.navigationBarColor = color
    activity?.applyNavigationBarColor(color)

    // Seek Progress background
    tvSeekProgress.setBackgroundColor(ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY))

    // Media Buttons Icon
    btnPrevious.setColorFilter(color)
    btnPlayPause.setColorFilter(color)
    btnNext.setColorFilter(color)

    // Media Buttons Ripple (need to use separate drawables)
    val rippleColor = ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY)
    btnPrevious.background = Theme.getRippleDrawable(rippleColor, mPreviousBackground)
    btnPlayPause.background = Theme.getRippleDrawable(rippleColor, mPlayPauseBackground)
    btnNext.background = Theme.getRippleDrawable(rippleColor, mNextBackground)
  }

  companion object {
    private const val LOG_TAG = "PlayerFragment"

    private const val FAST_FORWARD_INTERVAL = 500
  }
}
