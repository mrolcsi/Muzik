package hu.mrolcsi.android.lyricsplayer.library

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import hu.mrolcsi.android.lyricsplayer.GlideApp
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.artist
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration
import hu.mrolcsi.android.lyricsplayer.extensions.media.isPlaying
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToPreviousEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.startProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.media.stopProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.media.title
import hu.mrolcsi.android.lyricsplayer.extensions.mediaControllerCompat
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.player.PlayerViewModel
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_miniplayer.*

class MiniPlayerFragment : Fragment() {

  private lateinit var mPlayerModel: PlayerViewModel

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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activity?.let { activity ->
      mPlayerModel = ViewModelProviders.of(activity).get(PlayerViewModel::class.java).apply {

        mediaController.observe(this@MiniPlayerFragment, Observer { controller ->
          controller?.let { setupControls(controller) }
        })

        currentPlaybackState.observe(this@MiniPlayerFragment, Observer { state ->
          state?.let { updateControls(state) }
        })

        currentMediaMetadata.observe(this@MiniPlayerFragment, Observer { metadata ->
          metadata?.let { updateMetadata(metadata) }
        })
      }
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(this, Observer {
      applyTheme(it)
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_miniplayer, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    view.setOnClickListener {
      // Shared Element Transition
      val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
        requireActivity(),
        Pair.create(imgCoverArt, ViewCompat.getTransitionName(imgCoverArt))
      )
      startActivity(
        Intent(requireContext(), PlayerActivity::class.java),
        options.toBundle()
      )
      // Navigation Controller loses current destination
      // when opening Activity through NavController.navigate(destination)
    }

  }

  override fun onStop() {
    super.onStop()

    activity?.mediaControllerCompat?.transportControls?.stopProgressUpdater()
  }

  private fun setupControls(controller: MediaControllerCompat) {
    // Update song metadata
    controller.metadata?.let {
      updateMetadata(it)
    }

    // Update music controls
    controller.playbackState?.let {
      updateControls(it)
    }

    // Setup listeners

    btnPrevious.setOnClickListener {
      if (pbSongProgress.progress > 5) {
        // restart the song
        controller.transportControls?.seekTo(0)
      } else {
        controller.transportControls?.skipToPrevious()
      }
    }

    btnNext.setOnClickListener {
      controller.transportControls?.skipToNext()
    }

    btnPlayPause.setOnClickListener {
      when (controller.playbackState.state) {
        PlaybackStateCompat.STATE_PLAYING -> {
          // Pause playback, stop updater
          controller.transportControls.pause()
          controller.transportControls.startProgressUpdater()
        }
        PlaybackStateCompat.STATE_PAUSED,
        PlaybackStateCompat.STATE_STOPPED -> {
          // Start playback, start updater
          controller.transportControls.play()
          controller.transportControls.stopProgressUpdater()
        }
      }
    }
  }

  @SuppressLint("SetTextI18n")
  private fun updateControls(playbackState: PlaybackStateCompat) {
    // Update progress
    val elapsedTime = playbackState.position / 1000
    pbSongProgress.progress = elapsedTime.toInt()

    btnPrevious.isEnabled = playbackState.isSkipToPreviousEnabled
    btnPrevious.alpha = if (playbackState.isSkipToPreviousEnabled) 1f else Theme.DISABLED_ALPHA

    btnNext.isEnabled = playbackState.isSkipToNextEnabled
    btnNext.alpha = if (playbackState.isSkipToNextEnabled) 1f else Theme.DISABLED_ALPHA

    when (playbackState.isPlaying) {
      true -> {
        activity?.mediaControllerCompat?.transportControls?.startProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
      }
      false -> {
        activity?.mediaControllerCompat?.transportControls?.stopProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
      }
    }
  }

  private fun updateMetadata(metadata: MediaMetadataCompat) {
    btnPrevious.visibility = View.VISIBLE
    btnPlayPause.visibility = View.VISIBLE
    btnNext.visibility = View.VISIBLE

    GlideApp.with(this)
      .load(metadata.albumArt)
      .into(imgCoverArt)

    pbSongProgress.max = (metadata.duration / 1000).toInt()

    tvTitle.text = metadata.title
    tvArtist.text = metadata.artist
  }

  private fun applyTheme(theme: Theme) {

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme

    ValueAnimator.ofArgb(
      previousTheme?.primaryBackgroundColor ?: ContextCompat.getColor(requireContext(), R.color.backgroundColor),
      theme.primaryBackgroundColor
    ).run {
      duration = Theme.PREFERRED_ANIMATION_DURATION
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
      duration = Theme.PREFERRED_ANIMATION_DURATION
      addUpdateListener {
        val color = it.animatedValue as Int

        applyForegroundColor(color)
      }
      start()
    }

    val ripple = Theme.getRippleDrawable(theme.primaryForegroundColor, theme.primaryBackgroundColor)
    (view as FrameLayout).foreground = ripple
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