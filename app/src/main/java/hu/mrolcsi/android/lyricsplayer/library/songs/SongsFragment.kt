package hu.mrolcsi.android.lyricsplayer.library.songs

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.OnItemClickListener
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_browser.*

class SongsFragment : Fragment() {

  private val args: SongsFragmentArgs by navArgs()

  private lateinit var mSongsModel: SongsViewModel

  private val mSongsAdapter = SongsAdapter(OnItemClickListener { item, holder, position, id ->
    MediaControllerCompat.getMediaController(requireActivity())
      .transportControls
      .playFromMediaId(item.mediaId, null)
  })

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activity?.let {
      mSongsModel = ViewModelProviders.of(requireActivity()).get(SongsViewModel::class.java)
      mSongsModel.getSongs().observe(this, Observer { songs ->
        mSongsAdapter.submitList(songs)
      })
    }

    ThemeManager.currentTheme.observe(this, Observer {
      // Tell adapter to reload its views
      mSongsAdapter.notifyDataSetChanged()
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_browser, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mSongsAdapter
  }

  override fun onResume() {
    super.onResume()

    if (arguments != null) {
      when {
        args.albumKey != null -> {
          // List songs from an album
          mSongsAdapter.showTrackNumber = true
          mSongsModel.songFilter.value = SongsViewModel.SongFilter(albumKey = args.albumKey)
        }
        args.artistKey != null -> {
          // List all songs by an artist
          mSongsAdapter.showTrackNumber = false
          mSongsModel.songFilter.value = SongsViewModel.SongFilter(artistKey = args.artistKey)
        }
        else -> {
          mSongsAdapter.showTrackNumber = false
          mSongsModel.songFilter.value = SongsViewModel.SongFilter.NO_FILTER
        }
      }
    } else {
      mSongsAdapter.showTrackNumber = false
      mSongsModel.songFilter.value = SongsViewModel.SongFilter.NO_FILTER
    }
  }
}