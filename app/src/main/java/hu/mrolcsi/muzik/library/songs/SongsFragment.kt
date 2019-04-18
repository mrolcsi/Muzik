package hu.mrolcsi.muzik.library.songs

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.extensions.media.addQueueItems
import hu.mrolcsi.muzik.extensions.media.playFromDescription
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.muzik.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_songs.*

class SongsFragment : Fragment() {

  private val args: SongsFragmentArgs by navArgs()

  private lateinit var mSongsModel: SongsViewModel

  private lateinit var mVisibleSongs: List<MediaBrowserCompat.MediaItem>

  private val mSongsAdapter = SongsAdapter(OnItemClickListener { item, holder, position, id ->
    Log.d(LOG_TAG, "onItemClicked($item, $holder, $position, $id)")

    val controller = MediaControllerCompat.getMediaController(requireActivity())

    // Immediately start the song that was clicked on
    controller.transportControls.playFromDescription(
      item.description,
      bundleOf(ExoPlayerHolder.EXTRA_DESIRED_QUEUE_POSITION to position)
    )

    AsyncTask.execute {
      Log.d(LOG_TAG, "onItemClicked() Collecting descriptions...")

      // Add songs to queue
      val descriptions = mVisibleSongs.filterIndexed { index, _ ->
        index != position
      }.map {
        it.description
      }

      Log.d(LOG_TAG, "onItemClicked() Sending items to queue...")

      controller.addQueueItems(descriptions)
    }
  })

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    activity?.run {
      mSongsModel = ViewModelProviders.of(this).get(SongsViewModel::class.java)
      mSongsModel.getSongs().observe(viewLifecycleOwner, Observer { songs ->
        mSongsAdapter.submitList(songs)
        mVisibleSongs = songs
      })
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, Observer {
      // Tell adapter to reload its views
      mSongsAdapter.notifyDataSetChanged()
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_songs, container, false)
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

  companion object {
    private const val LOG_TAG = "SongsFragment"
  }
}