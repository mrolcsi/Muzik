package hu.mrolcsi.muzik.library.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_artists.*

class ArtistsFragment : Fragment() {

  private val mArtistAdapter = ArtistsAdapter()

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    activity?.run {
      val model = ViewModelProviders.of(this).get(ArtistsViewModel::class.java)
      model.getArtists().observe(viewLifecycleOwner, Observer { artists ->
        mArtistAdapter.submitList(artists)
      })
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, Observer {
      // Tell adapter to reload its views
      mArtistAdapter.notifyDataSetChanged()
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_artists, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvBrowser.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    rvBrowser.adapter = mArtistAdapter
  }
}