package hu.mrolcsi.muzik.player.playlist

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.player.PlayerViewModel
import hu.mrolcsi.muzik.service.extensions.media.isPlaying
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_playlist.*

class PlaylistFragment : Fragment() {

  private lateinit var mPlayerModel: PlayerViewModel

  private val mPlaylistAdapter =
    PlaylistAdapter(OnItemClickListener { _, _, _, id ->
      // Skip to clicked item
      val controller = MediaControllerCompat.getMediaController(requireActivity())
      controller.transportControls.skipToQueueItem(id)
      controller.transportControls.play()
    })

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    activity?.run {
      mPlayerModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
        Log.d(LOG_TAG, "Got PlayerViewModel: $this")


        currentPlaybackState.observe(viewLifecycleOwner, object : Observer<PlaybackStateCompat?> {

          private var previousState: PlaybackStateCompat? = null

          override fun onChanged(it: PlaybackStateCompat?) {
            if (it != null) {
              if (previousState?.state != it.state) {
                previousState = it
                mPlaylistAdapter.isPlaying = it.isPlaying
              }
            }
          }
        })

        currentMediaMetadata.observe(viewLifecycleOwner, Observer { metadata ->
          metadata?.let {
            // Update active queueId in adapter (notifyDataSetChanged will do the rest)
            MediaControllerCompat.getMediaController(requireActivity())?.let { controller ->
              mPlaylistAdapter.activeQueueId = controller.playbackState.activeQueueItemId

              val layoutManager = rvPlaylist.layoutManager as LinearLayoutManager
              val activePosition = controller.playbackState.activeQueueItemId.toInt()
              val first = layoutManager.findFirstCompletelyVisibleItemPosition()
              val last = layoutManager.findLastCompletelyVisibleItemPosition()

              if (activePosition !in first..last) {
                layoutManager.scrollToPositionWithOffset(activePosition, 500)
              }
            }
          }
        })
      }
    }

    PlayQueueDatabase.getInstance(requireContext())
      .getPlayQueueDao()
      .fetchQueue()
      .observe(this, Observer {
        mPlaylistAdapter.submitList(it)
      })

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, object : Observer<Theme> {

      private var initialLoad = true

      override fun onChanged(theme: Theme) {
        if (initialLoad) {
          applyThemeStatic(theme)
          initialLoad = false
        } else {
          applyThemeAnimated(theme)
        }
      }
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_playlist, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvPlaylist.apply {
      adapter = mPlaylistAdapter
      //addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    // TODO: Dynamic title
    playlistToolbar.setTitle(R.string.playlist_title)
  }

  private fun applyThemeStatic(theme: Theme) {
    applyBackgroundColor(theme.primaryBackgroundColor)
    applyForegroundColor(theme.primaryForegroundColor)
  }

  private fun applyThemeAnimated(theme: Theme) {

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme
    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    // Background Color
    ValueAnimator.ofArgb(
      previousTheme?.primaryBackgroundColor ?: ContextCompat.getColor(
        requireContext(),
        R.color.backgroundColor
      ),
      theme.primaryBackgroundColor
    ).apply {
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
    ).apply {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        applyForegroundColor(color)
      }
      start()
    }
  }

  private fun applyBackgroundColor(color: Int) {
    rvPlaylist?.setBackgroundColor(color)
    mPlaylistAdapter.notifyDataSetChanged()
    playlistToolbar?.setBackgroundColor(color)
  }

  private fun applyForegroundColor(color: Int) {
    playlistToolbar?.setTitleTextColor(color)
    for (i in 0 until playlistToolbar.menu.size()) {
      playlistToolbar.menu[i].icon.setTint(color)
    }
  }

  companion object {
    private const val LOG_TAG = "PlaylistFragment"
  }

}