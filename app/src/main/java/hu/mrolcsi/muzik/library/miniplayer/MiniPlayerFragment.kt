package hu.mrolcsi.muzik.library.miniplayer

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.OnSwipeTouchListener
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentMiniplayerBinding
import hu.mrolcsi.muzik.extensions.mediaControllerCompat
import hu.mrolcsi.muzik.extensions.startMarquee
import hu.mrolcsi.muzik.service.extensions.media.stopProgressUpdater
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_miniplayer.*
import javax.inject.Inject

class MiniPlayerFragment : DaggerFragment() {

  @Inject lateinit var viewModel: MiniPlayerViewModel

  // Prepare drawables (separate for each button)
  private val mPreviousBackground by lazy {
    ContextCompat.getDrawable(
      requireContext(),
      R.drawable.media_button_background
    )
  }
  private val mPlayPauseBackground by lazy {
    ContextCompat.getDrawable(
      requireContext(),
      R.drawable.media_button_background
    )
  }
  private val mNextBackground by lazy {
    ContextCompat.getDrawable(
      requireContext(),
      R.drawable.media_button_background
    )
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      NavHostFragment.findNavController(requireParentFragment()).observeAndRunNavCommands(viewLifecycleOwner, this)
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, Observer {
      applyTheme(it)
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    FragmentMiniplayerBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val marqueeDelay = requireContext().resources.getInteger(R.integer.preferredMarqueeDelay).toLong()

    // Enable marquee
    tvTitle.startMarquee(marqueeDelay)
    tvArtist.startMarquee(marqueeDelay)

    view.setOnClickListener {
      viewModel.openPlayer(imgCoverArt)
    }
    view.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
      override fun onSwipeDown() {
        viewModel.openPlayer(imgCoverArt)
      }
    })
  }

  override fun onStop() {
    super.onStop()

    activity?.mediaControllerCompat?.transportControls?.stopProgressUpdater()
  }

  private fun applyTheme(theme: Theme) {

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme
    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

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

    val ripple = Theme.getRippleDrawable(theme.primaryForegroundColor, theme.primaryBackgroundColor)
    (view as? FrameLayout)?.foreground = ripple
  }

  private fun applyBackgroundColor(color: Int) {
    view?.setBackgroundColor(color)

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

  private fun applyForegroundColor(color: Int) {
    tvTitle.setTextColor(color)
    tvArtist.setTextColor(color)

    pbSongProgress.progressDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

    mPreviousBackground?.setTint(color)
    mPlayPauseBackground?.setTint(color)
    mNextBackground?.setTint(color)
  }
}