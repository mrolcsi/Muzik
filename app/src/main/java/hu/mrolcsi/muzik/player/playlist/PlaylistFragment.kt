package hu.mrolcsi.muzik.player.playlist

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.view.MVVMListAdapter
import hu.mrolcsi.muzik.databinding.FragmentPlaylistBinding
import hu.mrolcsi.muzik.extensions.applyForegroundColor
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_playlist.*
import javax.inject.Inject

class PlaylistFragment : DaggerFragment() {

  @Inject lateinit var viewModel: PlaylistViewModel

  private val playlistAdapter = MVVMListAdapter(
    diffCallback = PlaylistItem.DIFF_CALLBACK,
    itemIdSelector = { it.entry._id },
    viewHolderFactory = { parent, _ ->
      PlaylistItemHolder(parent).apply {
        itemView.setOnClickListener {
          model?.let { viewModel.onSelect(it) }
        }
      }
    }
  )

  private val divider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL).apply {
      setDrawable(resources.getDrawable(R.drawable.list_divider_inset, requireContext().theme))
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.items.observe(viewLifecycleOwner, playlistAdapter)

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

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentPlaylistBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvPlaylist.apply {
      addItemDecoration(divider)
      adapter = playlistAdapter
    }
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
    playlistAdapter.notifyDataSetChanged()
    playlistToolbar?.setBackgroundColor(color)
  }

  private fun applyForegroundColor(color: Int) {
    playlistToolbar.applyForegroundColor(color)
    divider.setTint(color)
  }

}