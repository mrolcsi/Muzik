package hu.mrolcsi.android.lyricsplayer.player

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_artists.*

class PlaylistFragment : Fragment() {

  private lateinit var mPlayerModel: PlayerViewModel

  private val mPlaylistAdapter = PlaylistAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activity?.let {
      mPlayerModel = ViewModelProviders.of(it).get(PlayerViewModel::class.java).apply {
        Log.d(LOG_TAG, "Got PlayerViewModel: $this")

        mediaController.observe(this@PlaylistFragment, Observer { controller ->
          controller?.let {
            // Update playlist adapter
            mPlaylistAdapter.submitList(controller.queue)
          }
        })

        currentMediaMetadata.observe(this@PlaylistFragment, Observer { metadata ->
          // Update playlist adapter
          val controller = MediaControllerCompat.getMediaController(it)
          mPlaylistAdapter.submitList(controller.queue)
        })
      }
    }

    ThemeManager.currentTheme.observe(this, Observer { theme ->
      view?.setBackgroundColor(theme.primaryBackgroundColor)
      mPlaylistAdapter.notifyDataSetChanged()
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_playlist, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mPlaylistAdapter
  }

  companion object {
    private const val LOG_TAG = "PlaylistFragment"
  }

}