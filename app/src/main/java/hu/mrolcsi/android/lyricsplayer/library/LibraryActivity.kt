package hu.mrolcsi.android.lyricsplayer.library

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.ColorUtils
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.palette.graphics.Palette
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.isPermissionGranted
import hu.mrolcsi.android.lyricsplayer.extensions.requestPermission
import hu.mrolcsi.android.lyricsplayer.extensions.shouldShowPermissionRationale
import hu.mrolcsi.android.lyricsplayer.library.albums.AlbumsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.library.songs.SongsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.player.PlayerViewModel
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity() {

  private lateinit var mPlayerModel: PlayerViewModel

  private var mNowPlayingMenuItem: MenuItem? = null
  private var mNowPlayingCoverArt: ImageView? = null
  private var mNowPlayingIcon: ImageView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_library)
    setSupportActionBar(libraryToolbar)

    if (savedInstanceState == null) {
      // Initial loading
      if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
        Log.d(LOG_TAG, "No permission yet. Request from user.")
        navigation_bar.visibility = View.GONE
        requestStoragePermission()
      } else {
        Log.d(LOG_TAG, "Permission already granted. Loading Library.")
        createNavHost()
      }
    }

    mPlayerModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
      currentMediaMetadata.observe(this@LibraryActivity, Observer { metadata ->
        // Update cover art in ActionBar
        metadata?.let {
          mNowPlayingCoverArt?.setImageBitmap(metadata.description?.iconBitmap)
          Palette.from(metadata.albumArt).generate { palette ->
            palette?.dominantSwatch?.let { swatch ->
              // Window Background, Status Bar and Navigation
              val darkRgb = ColorUtils.setAlphaComponent(swatch.rgb, 76)
              window?.apply {
                statusBarColor = swatch.rgb
                navigationBarColor = darkRgb
              }
              // Fragment Background
              library_nav_host.setBackgroundColor(palette.getDarkMutedColor(Color.BLACK))
              // Toolbar Background
              libraryToolbar.setBackgroundColor(swatch.rgb)
              // Toolbar Icon
              libraryToolbar.navigationIcon?.setColorFilter(swatch.bodyTextColor, PorterDuff.Mode.SRC_IN)
              // Title and Subtitle
              libraryToolbar.setTitleTextColor(swatch.bodyTextColor)
              libraryToolbar.setSubtitleTextColor(swatch.titleTextColor)
              // Toolbar Now Playing Icon
              mNowPlayingIcon?.imageTintList =
                ColorStateList.valueOf(swatch.bodyTextColor)
              // BottomNavigation Background
              navigation_bar.setBackgroundColor(darkRgb)
              // BottomNavigation Selected Colors
              val navigationTintList = ColorStateList(
                arrayOf(
                  intArrayOf(android.R.attr.state_checked),
                  intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                  swatch.bodyTextColor,
                  ColorUtils.setAlphaComponent(swatch.bodyTextColor, 76)
                )
              )
              navigation_bar.itemIconTintList = navigationTintList
              navigation_bar.itemTextColor = navigationTintList
            }
          }
        }
      })
    }
  }

  override fun onStart() {
    super.onStart()
    mPlayerModel.connect()
  }

  override fun onStop() {
    super.onStop()
    mPlayerModel.disconnect()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_library, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    menu?.let {
      mNowPlayingMenuItem = menu.findItem(R.id.menuNowPlaying).also { item ->
        with(item.actionView) {
          setOnClickListener { onOptionsItemSelected(item) }
          mNowPlayingCoverArt = this.findViewById(R.id.imgCoverArt)
          mNowPlayingIcon = this.findViewById(R.id.imgPlay)
        }
      }
    }

    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      R.id.menuNowPlaying -> {
        // Shared Element Transition
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
          this,
          Pair.create(mNowPlayingCoverArt, ViewCompat.getTransitionName(mNowPlayingCoverArt!!))
        )
        startActivity(
          Intent(this, PlayerActivity::class.java),
          options.toBundle()
        )
        // Navigation Controller loses current destination
        // when opening Activity through NavController.navigate(destination)
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun requestStoragePermission(view: View? = null) {
    if (shouldShowPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      Log.d(LOG_TAG, "Showing rationale for permission.")

      groupPermissionHint.visibility = View.VISIBLE
      navigation_bar.visibility = View.GONE

      requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE)
    } else {
      Log.d(LOG_TAG, "No need to show rationale. Request permission.")
      requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE)
    }
  }

  private fun createNavHost() {
    Log.d(LOG_TAG, "Creating navigation host.")

    // hide hints, show navigation
    groupPermissionHint.visibility = View.GONE

    // enable navigation
    val finalHost = NavHostFragment.create(R.navigation.library_navigation)
    supportFragmentManager.beginTransaction()
      .replace(R.id.library_nav_host, finalHost)
      .setPrimaryNavigationFragment(finalHost)
      .runOnCommit {
        setupNavBar(findNavController(R.id.library_nav_host))
      }
      .commit()
  }

  private fun setupNavBar(navController: NavController) {
    val appBarConfig = AppBarConfiguration.Builder(
      R.id.navigation_artists,
      R.id.navigation_albums,
      R.id.navigation_songs
    ).build()

    NavigationUI.setupWithNavController(libraryToolbar, navController, appBarConfig)
    NavigationUI.setupWithNavController(navigation_bar, navController)

    navController.addOnDestinationChangedListener { _, destination, arguments ->
      when (destination.id) {
        R.id.navigation_artists -> {
          libraryToolbar.subtitle = null
        }
        R.id.navigation_albums -> {
          libraryToolbar.subtitle = null
        }
        R.id.navigation_albumsByArtist -> {
          if (arguments != null) {
            val args = AlbumsFragmentArgs.fromBundle(arguments)
            libraryToolbar.subtitle = getString(R.string.albums_byArtist_subtitle, args.artistName)
          }
        }
        R.id.navigation_songs -> {
          libraryToolbar.subtitle = null
        }
        R.id.navigation_songsFromAlbum -> {
          if (arguments != null) {
            val args = SongsFragmentArgs.fromBundle(arguments)
            if (args.albumKey != null) {
              libraryToolbar.subtitle = getString(R.string.songs_fromAlbum_subtitle, args.albumTitle)
            } else if (args.artistKey != null) {
              libraryToolbar.subtitle = getString(R.string.albums_byArtist_subtitle, args.artistName)
            }
          }
        }
      }
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    return findNavController(R.id.library_nav_host).navigateUp() || super.onSupportNavigateUp()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    when (requestCode) {
      PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE -> {
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          // permission was granted, yay! Do the
          // contacts-related task you need to do.
          Log.d(LOG_TAG, "Permission granted. Loading Library.")
          createNavHost()
        } else {
          // permission denied, boo! Disable the
          // functionality that depends on this permission.
          Log.d(LOG_TAG, "Permission denied.")
          groupPermissionHint.visibility = View.VISIBLE
        }
        return
      }

      // Add other 'when' lines to check for other
      // permissions this app might request.
      else -> {
        // Ignore all other requests.
      }
    }
  }

  companion object {
    const val LOG_TAG = "LibraryActivity"

    const val PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE = 9514
  }
}
