package hu.mrolcsi.muzik.library.songs

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.muzik.service.extensions.media.addQueueItems
import hu.mrolcsi.muzik.service.extensions.media.playFromDescription
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_songs.*

class SongsFragment : Fragment() {

  private lateinit var mSongsModel: SongsViewModel

  private lateinit var mVisibleSongs: List<MediaBrowserCompat.MediaItem>

  private val mDivider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL).apply {
      setDrawable(resources.getDrawable(R.drawable.list_divider_inset, requireContext().theme))
    }
  }

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

      mDivider.setTint(it.tertiaryForegroundColor)
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_songs, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvSongs.apply {
      adapter = mSongsAdapter
      addItemDecoration(mDivider)
    }
  }

  companion object {
    private const val LOG_TAG = "SongsFragment"
  }
}