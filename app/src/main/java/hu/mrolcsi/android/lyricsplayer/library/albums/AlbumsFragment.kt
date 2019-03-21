package hu.mrolcsi.android.lyricsplayer.library.albums

import android.os.Bundle
import android.util.Log
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
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_albums.*

// see: https://stackoverflow.com/a/53999441

class AlbumsFragment : Fragment() {

  private val args: AlbumsFragmentArgs by navArgs()

  private lateinit var mAlbumsModel: AlbumsViewModel

  private var mAlbumsAdapter: AlbumsAdapter = AlbumsAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activity?.let {
      mAlbumsModel = ViewModelProviders.of(requireActivity()).get(AlbumsViewModel::class.java)
      mAlbumsModel.albums.observe(this, Observer { albums ->
        Log.d(LOG_TAG, "Got items from LiveData: $albums")
        mAlbumsAdapter.submitList(albums)
      })
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(this, Observer {
      // Tell adapter to reload its views
      mAlbumsAdapter.notifyDataSetChanged()
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_albums, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mAlbumsAdapter
  }

  override fun onResume() {
    super.onResume()

    if (arguments != null) {
      if (args.artistName != null) {
        mAlbumsModel.artistFilter.value =
          AlbumsViewModel.ArtistInfo(args.artistKey, args.artistName, args.numberOfTracks)
      } else {
        mAlbumsModel.artistFilter.value = null
      }
    } else {
      mAlbumsModel.artistFilter.value = null
    }
  }

  companion object {
    private const val LOG_TAG = "AlbumsFragment"
  }
}