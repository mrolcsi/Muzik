package hu.mrolcsi.muzik.theme

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.ActivityThemeTestBinding
import hu.mrolcsi.muzik.ui.miniPlayer.MiniPlayerViewModel
import hu.mrolcsi.muzik.ui.miniPlayer.MiniPlayerViewModelImpl
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.debug.activity_theme_test.*
import kotlinx.android.synthetic.debug.list_item_swatch.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import top.defaults.checkerboarddrawable.CheckerboardDrawable

class ThemeTestActivity : AppCompatActivity() {

  private val viewModel: MiniPlayerViewModel by viewModel<MiniPlayerViewModelImpl>()

  // Adapters
  private val mAllColorsAdapter = PaletteAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DataBindingUtil.setContentView<ActivityThemeTestBinding>(this, R.layout.activity_theme_test).also {
      it.lifecycleOwner = this
      it.viewModel = viewModel
    }

    root.background = CheckerboardDrawable.create()

    viewModel.coverArtUri.observe(this, Observer { uri ->
      Picasso.get()
        .load(uri)
        .into(imgInput)
    })

    viewModel.currentTheme.observe(this, Observer { theme ->
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
      tvPrimary.setBackgroundColor(theme.backgroundColor)
      tvPrimary.setTextColor(theme.foregroundColor)
    })

    rvAllColors.apply {
      adapter = mAllColorsAdapter
    }
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
      val item = getItem(position)!!
      holder.tvSwatch.setBackgroundColor(item.rgb)
      holder.tvSwatch.setTextColor(item.titleTextColor)
      val rgbText = "R=${Color.red(item.rgb)}\nG=${Color.green(item.rgb)}\nB=${Color.blue(item.rgb)}"
      val hslText = "H=${item.hsl[0]}\nS=${item.hsl[1]}\nL=${item.hsl[2]}"
      holder.tvSwatch.text = rgbText
      holder.tvSwatch.setOnClickListener {
        if (holder.tvSwatch.text == rgbText)
          holder.tvSwatch.text = hslText
        else holder.tvSwatch.text = rgbText
      }
    }

    private class SwatchHolder(override val containerView: View) :
      RecyclerView.ViewHolder(containerView), LayoutContainer
  }
}