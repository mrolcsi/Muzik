package hu.mrolcsi.android.lyricsplayer.player.playlist

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.OnItemClickListener
import hu.mrolcsi.android.lyricsplayer.player.PlayerViewModel
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_playlist.*

class PlaylistFragment : Fragment() {

  private lateinit var mPlayerModel: PlayerViewModel

  private val mPlaylistAdapter =
    PlaylistAdapter(OnItemClickListener { item, holder, position, id ->
      // Skip to clicked item
      val controller = MediaControllerCompat.getMediaController(requireActivity())
      controller.transportControls.skipToQueueItem(id)
      controller.transportControls.play()
    })

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activity?.let {
      mPlayerModel = ViewModelProviders.of(it).get(PlayerViewModel::class.java).apply {
        Log.d(LOG_TAG, "Got PlayerViewModel: $this")

        currentPlaybackState.observe(this@PlaylistFragment, Observer { state ->
          state?.let {
            // Update active queueId in adapter (notifyDataSetChanged will do the rest)
            mPlaylistAdapter.activeQueueId = state.activeQueueItemId
          }
        })

        currentMediaMetadata.observe(this@PlaylistFragment, Observer { metadata ->
          // Update playlist adapter
          val controller = MediaControllerCompat.getMediaController(it)
          Log.v(LOG_TAG, "Updating playlist: Size=${controller.queue?.size}")
          mPlaylistAdapter.submitList(controller.queue)
        })
      }
    }

    ThemeManager.currentTheme.observe(this, Observer { theme ->
      applyTheme(theme)
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_playlist, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mPlaylistAdapter
    rvBrowser.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

    playlistToolbar.setTitle(R.string.playlist_title)
  }

  private fun applyTheme(theme: Theme) {

    // Background Color
    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.primaryBackgroundColor ?: ContextCompat.getColor(
        requireContext(),
        R.color.backgroundColor
      ),
      theme.primaryBackgroundColor
    ).apply {
      duration = Theme.PREFERRED_ANIMATION_DURATION
      addUpdateListener {
        val color = it.animatedValue as Int
        applyBackgroundColor(color)
      }
      start()
    }

    // Foreground Color
    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.primaryForegroundColor ?: Color.WHITE,
      theme.primaryForegroundColor
    ).apply {
      duration = Theme.PREFERRED_ANIMATION_DURATION
      addUpdateListener {
        val color = it.animatedValue as Int
        applyForegroundColor(color)
      }
      start()
    }
  }

  private fun applyBackgroundColor(color: Int) {
    rvBrowser.setBackgroundColor(color)
    mPlaylistAdapter.notifyDataSetChanged()
    playlistToolbar.setBackgroundColor(color)
  }

  private fun applyForegroundColor(color: Int) {
    playlistToolbar.setTitleTextColor(color)
    for (i in 0 until playlistToolbar.menu.size()) {
      playlistToolbar.menu[i].icon.setTint(color)
    }
  }

  companion object {
    private const val LOG_TAG = "PlaylistFragment"
  }

}