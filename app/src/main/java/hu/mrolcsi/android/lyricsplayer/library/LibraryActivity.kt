package hu.mrolcsi.android.lyricsplayer.library

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToNavigationBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToStatusBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.isPermissionGranted
import hu.mrolcsi.android.lyricsplayer.extensions.requestPermission
import hu.mrolcsi.android.lyricsplayer.extensions.shouldShowPermissionRationale
import hu.mrolcsi.android.lyricsplayer.library.albums.AlbumsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.library.songs.SongsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.player.PlayerViewModel
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity() {

  private lateinit var mPlayerModel: PlayerViewModel

  private var mNowPlayingMenuItem: MenuItem? = null
  private var mNowPlayingCoverArt: ImageView? = null
  private var mNowPlayingIcon: ImageView? = null

  private var mNavBarReady = false

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
      mediaController.observe(this@LibraryActivity, Observer { controller ->
        // Set mediaController to the Activity
        MediaControllerCompat.setMediaController(this@LibraryActivity, controller)

        // Update from state
        updateControls(controller?.playbackState)

        // Update from metadata
        updateSongData(controller?.metadata)
      })
      currentPlaybackState.observe(this@LibraryActivity, Observer { playbackState ->
        updateControls(playbackState)
      })
      currentMediaMetadata.observe(this@LibraryActivity, Observer { metadata ->
        updateSongData(metadata)
      })
    }

    ThemeManager.currentTheme.observe(this@LibraryActivity, Observer {
      applyTheme(it)
    })
  }

  override fun onStart() {
    super.onStart()
    mPlayerModel.connect()
  }

  override fun onResumeFragments() {
    super.onResumeFragments()

    if (!mNavBarReady) {
      try {
        setupNavBar(findNavController(R.id.library_nav_host))
      } catch (e: IllegalStateException) {
        // NavController not ready yet.
      }
    }

    // Apply StatusBar and NavigationBar colors again
    applyColorToStatusBarIcons(ThemeManager.currentTheme.value?.primaryBackgroundColor ?: Color.BLACK)
    applyColorToNavigationBarIcons(ThemeManager.currentTheme.value?.secondaryBackgroundColor ?: Color.BLACK)
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
    navigation_bar.visibility = View.VISIBLE

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

    mNavBarReady = true
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

  private fun updateSongData(metadata: MediaMetadataCompat?) {
    // Update cover art in ActionBar
    mNowPlayingCoverArt?.let { imgView ->
      metadata?.description?.iconBitmap.let { bitmap ->
        Glide.with(this)
          .load(bitmap)
          .transition(
            DrawableTransitionOptions.with(
              DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            )
          ).into(imgView)
      }
    }
  }

  private fun updateControls(playbackState: PlaybackStateCompat?) {
    // Show/hide Now Playing in ActionBar
    mNowPlayingMenuItem?.isVisible = playbackState != null
  }

  private fun applyTheme(theme: Theme) {
    Log.d(LOG_TAG, "Applying theme...")

    // Status Bar Icons
    applyColorToStatusBarIcons(theme.primaryBackgroundColor)
    // Navigation Bar Icons
    applyColorToNavigationBarIcons(theme.secondaryBackgroundColor)

    // Animate changes
    val animationDuration: Long = 500

    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.primaryBackgroundColor ?: Color.BLACK,
      theme.primaryBackgroundColor
    ).apply {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        // Status Bar
        window?.statusBarColor = color
        // Toolbar Background
        libraryToolbar.setBackgroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.secondaryBackgroundColor ?: Color.BLACK,
      theme.secondaryBackgroundColor
    ).apply {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        // Navigation Bar
        window?.navigationBarColor = color
        // BottomNavigation Background
        navigation_bar.setBackgroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.tertiaryBackgroundColor ?: Color.BLACK,
      theme.tertiaryBackgroundColor
    ).apply {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        // Window background
        window?.decorView?.setBackgroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.primaryForegroundColor ?: Color.WHITE,
      theme.primaryForegroundColor
    ).apply {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        // Toolbar Icon
        libraryToolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        // Title and Subtitle
        libraryToolbar.setTitleTextColor(color)
        libraryToolbar.setSubtitleTextColor(color)
        // Toolbar Now Playing Icon
        mNowPlayingIcon?.imageTintList =
          ColorStateList.valueOf(theme.primaryForegroundColor)
      }
      start()
    }

    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.secondaryForegroundColor ?: Color.WHITE,
      theme.secondaryForegroundColor
    ).apply {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int
        // BottomNavigation Selected Colors
        val navigationTintList = ColorStateList(
          arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
          ),
          intArrayOf(
            color,
            ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY)
          )
        )
        navigation_bar.itemIconTintList = navigationTintList
        navigation_bar.itemTextColor = navigationTintList
      }
      start()
    }
  }

  companion object {
    const val LOG_TAG = "LibraryActivity"

    const val PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE = 9514
  }
}
