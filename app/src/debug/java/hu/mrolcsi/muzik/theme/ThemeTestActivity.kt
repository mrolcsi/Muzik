package hu.mrolcsi.muzik.theme

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerAppCompatActivity
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.player.PlayerViewModel
import hu.mrolcsi.muzik.service.extensions.media.albumArt
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.debug.activity_theme_test.*
import kotlinx.android.synthetic.debug.list_item_swatch.*
import top.defaults.checkerboarddrawable.CheckerboardDrawable
import javax.inject.Inject

class ThemeTestActivity : DaggerAppCompatActivity() {

  @Inject lateinit var viewModel: PlayerViewModel

  // Adapters
  private val mAllColorsAdapter = PaletteAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_theme_test)

    root.background = CheckerboardDrawable.create()

    viewModel.apply {
      mediaController.observe(this@ThemeTestActivity, Observer {
        MediaControllerCompat.setMediaController(this@ThemeTestActivity, it)

        setupControls()
      })

      currentMediaMetadata.observe(this@ThemeTestActivity, Observer {
        it?.let { metadata ->
          imgInput.setImageBitmap(metadata.albumArt)
        }
      })
    }

    ThemeManager.getInstance(this).currentTheme.observe(this, Observer { theme ->
      // Source Palette
      theme.sourcePalette?.let { palette ->
        tvPaletteLightVibrant.setBackgroundColor(palette.lightVibrantSwatch?.rgb ?: Color.TRANSPARENT)
        tvPaletteVibrant.setBackgroundColor(palette.vibrantSwatch?.rgb ?: Color.TRANSPARENT)
        tvPaletteDarkVibrant.setBackgroundColor(palette.darkVibrantSwatch?.rgb ?: Color.TRANSPARENT)
        tvPaletteDominant.setBackgroundColor(palette.dominantSwatch?.rgb ?: Color.TRANSPARENT)
        tvPaletteLightMuted.setBackgroundColor(palette.lightMutedSwatch?.rgb ?: Color.TRANSPARENT)
        tvPaletteMuted.setBackgroundColor(palette.mutedSwatch?.rgb ?: Color.TRANSPARENT)
        tvPaletteDarkMuted.setBackgroundColor(palette.darkMutedSwatch?.rgb ?: Color.TRANSPARENT)

        // Palette by pixel count
        val paletteSorted = palette.swatches.sortedByDescending { it?.population ?: 0 }
        mAllColorsAdapter.submitList(paletteSorted)
      }

      // Generated Theme
      tvPrimary.setBackgroundColor(theme.primaryBackgroundColor)
      tvPrimary.setTextColor(theme.primaryForegroundColor)
      tvSecondary.setBackgroundColor(theme.secondaryBackgroundColor)
      tvSecondary.setTextColor(theme.secondaryForegroundColor)
      tvTertiary.setBackgroundColor(theme.tertiaryBackgroundColor)
      tvTertiary.setTextColor(theme.tertiaryForegroundColor)
    })

    rvAllColors.apply {
      layoutManager = LinearLayoutManager(this@ThemeTestActivity, RecyclerView.HORIZONTAL, false)
      adapter = mAllColorsAdapter
    }
  }

  private fun setupControls() {
    val controller = MediaControllerCompat.getMediaController(this)

    btnPrevious.setOnClickListener {
      controller.transportControls.skipToPrevious()
    }

    btnNext.setOnClickListener {
      controller.transportControls.skipToNext()
    }
  }

  override fun onStart() {
    super.onStart()

    viewModel.connect()
  }

  override fun onStop() {
    super.onStop()

    viewModel.disconnect()
  }

  private class PaletteAdapter : ListAdapter<Palette.Swatch?, PaletteAdapter.SwatchHolder>(
    object : DiffUtil.ItemCallback<Palette.Swatch?>() {
      override fun areItemsTheSame(oldItem: Palette.Swatch, newItem: Palette.Swatch): Boolean = oldItem == newItem

      override fun areContentsTheSame(oldItem: Palette.Swatch, newItem: Palette.Swatch): Boolean =
        oldItem.rgb == newItem.rgb
    }
  ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwatchHolder {
      val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.list_item_swatch, parent, false)
      return SwatchHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SwatchHolder, position: Int) {
      val item = getItem(position)
      holder.tvSwatch.setBackgroundColor(item?.rgb ?: Color.TRANSPARENT)
      holder.tvSwatch.setTextColor(item?.titleTextColor ?: Color.BLACK)
      holder.tvSwatch.text =
        "H=${item?.hsl?.get(0)?.toString()}\nS=${item?.hsl?.get(1)?.toString()}\nL=${item?.hsl?.get(2)?.toString()}"
    }

    private class SwatchHolder(override val containerView: View) :
      RecyclerView.ViewHolder(containerView), LayoutContainer
  }
}