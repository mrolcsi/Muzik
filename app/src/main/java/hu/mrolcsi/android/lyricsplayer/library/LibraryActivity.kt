package hu.mrolcsi.android.lyricsplayer.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.ActivityNavigatorExtras
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import hu.mrolcsi.android.lyricsplayer.LibraryNavigationDirections
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.isPermissionGranted
import hu.mrolcsi.android.lyricsplayer.extensions.requestPermission
import hu.mrolcsi.android.lyricsplayer.extensions.shouldShowPermissionRationale
import hu.mrolcsi.android.lyricsplayer.library.albums.AlbumsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.library.songs.SongsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.player.PlayerViewModel
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity() {

  private lateinit var mPlayerModel: PlayerViewModel

  private var mNowPlayingMenuItem: MenuItem? = null
  private var mNowPlayingCoverArt: ImageView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_library)

    if (savedInstanceState == null) {
      // Initial loading
      if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
        Log.d(TAG, "No permission yet. Request from user.")
        navigation_bar.visibility  = View.GONE
        requestStoragePermission()
      } else {
        Log.d(TAG, "Permission already granted. Loading Library.")
        createNavHost()
      }
    }

    mPlayerModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
      currentMediaMetadata.observe(this@LibraryActivity, Observer { metadata ->
        // Update cover art in ActionBar
        mNowPlayingCoverArt?.setImageBitmap(metadata?.description?.iconBitmap)
      })
      mediaController.observe(this@LibraryActivity, Observer {
        // Update everything
        mNowPlayingCoverArt?.setImageBitmap(it?.metadata?.description?.iconBitmap)
      })
    }
  }

  override fun onStart() {
    super.onStart()
    mPlayerModel.connect()
  }

  override fun onResumeFragments() {
    super.onResumeFragments()

//    try {
//      // Recreate navigation bar listeners
//      setupNavBar(findNavController(R.id.library_nav_host))
//    } catch (e: IllegalStateException) {
//      // NavHost has not yet been initialized.
//    }
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
        val extras = ActivityNavigatorExtras(options)
        findNavController(R.id.library_nav_host).navigate(
          LibraryNavigationDirections.actionGlobalToPlayer(),
          extras
        )
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun requestStoragePermission(view: View? = null) {
    if (shouldShowPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      Log.d(TAG, "Showing rationale for permission.")

      groupPermissionHint.visibility = View.VISIBLE
      navigation_bar.visibility = View.GONE

      requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE)
    } else {
      Log.d(TAG, "No need to show rationale. Request permission.")
      requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE)
    }
  }

  private fun createNavHost() {
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
    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)
    NavigationUI.setupWithNavController(navigation_bar, navController)

    navController.addOnDestinationChangedListener { _, destination, arguments ->
      when (destination.id) {
        R.id.navigation_artists -> {
          supportActionBar?.subtitle = null
        }
        R.id.navigation_albums -> {
          supportActionBar?.subtitle = null
        }
        R.id.navigation_albumsByArtist -> {
          if (arguments != null) {
            val args = AlbumsFragmentArgs.fromBundle(arguments)
            supportActionBar?.subtitle = getString(R.string.albums_byArtist_subtitle, args.artistName)
          }
        }
        R.id.navigation_songs -> {
          supportActionBar?.subtitle = null
        }
        R.id.navigation_songsFromAlbum -> {
          if (arguments != null) {
            val args = SongsFragmentArgs.fromBundle(arguments)
            if (args.albumKey != null) {
              supportActionBar?.subtitle = getString(R.string.songs_fromAlbum_subtitle, args.albumTitle)
            } else if (args.artistKey != null) {
              supportActionBar?.subtitle = getString(R.string.albums_byArtist_subtitle, args.artistName)
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
          Log.d(TAG, "Permission granted. Loading Library.")
          createNavHost()
        } else {
          // permission denied, boo! Disable the
          // functionality that depends on this permission.
          Log.d(TAG, "Permission denied.")
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
    const val TAG = "LibraryActivity"

    const val PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE = 9514
  }
}
