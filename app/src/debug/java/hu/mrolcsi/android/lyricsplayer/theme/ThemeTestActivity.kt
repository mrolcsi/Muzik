package hu.mrolcsi.android.lyricsplayer.theme

import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.player.PlayerViewModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.debug.activity_theme_test.*
import kotlinx.android.synthetic.debug.list_item_swatch.*
import top.defaults.checkerboarddrawable.CheckerboardDrawable

class ThemeTestActivity : AppCompatActivity() {

  private lateinit var mModel: PlayerViewModel

  // Adapters
  private val mAllColorsAdapter = PaletteAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_theme_test)

    root.background = CheckerboardDrawable.create()

    mModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
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

    ThemeManager.currentTheme.observe(this, Observer { theme ->
      // Source Palette
      tvPaletteLightVibrant.setBackgroundColor(theme.sourcePalette.lightVibrantSwatch?.rgb ?: Color.TRANSPARENT)
      tvPaletteVibrant.setBackgroundColor(theme.sourcePalette.vibrantSwatch?.rgb ?: Color.TRANSPARENT)
      tvPaletteDarkVibrant.setBackgroundColor(theme.sourcePalette.darkVibrantSwatch?.rgb ?: Color.TRANSPARENT)
      tvPaletteDominant.setBackgroundColor(theme.sourcePalette.dominantSwatch?.rgb ?: Color.TRANSPARENT)
      tvPaletteLightMuted.setBackgroundColor(theme.sourcePalette.lightMutedSwatch?.rgb ?: Color.TRANSPARENT)
      tvPaletteMuted.setBackgroundColor(theme.sourcePalette.mutedSwatch?.rgb ?: Color.TRANSPARENT)
      tvPaletteDarkMuted.setBackgroundColor(theme.sourcePalette.darkMutedSwatch?.rgb ?: Color.TRANSPARENT)

      // Palette by pixel count
      val paletteSorted = theme.sourcePalette.swatches.sortedByDescending { it?.population ?: 0 }
      mAllColorsAdapter.submitList(paletteSorted)

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

    mModel.connect()
  }

  override fun onStop() {
    super.onStop()

    mModel.disconnect()
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