package hu.mrolcsi.android.lyricsplayer.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.isPermissionGranted
import hu.mrolcsi.android.lyricsplayer.extensions.requestPermission
import hu.mrolcsi.android.lyricsplayer.extensions.shouldShowPermissionRationale
import hu.mrolcsi.android.lyricsplayer.library.albums.AlbumsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.library.songs.SongsFragmentArgs
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity() {

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
        loadNavHost()
      }
    }
  }

  fun requestStoragePermission(view: View? = null) {
    if (shouldShowPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      Log.d(TAG, "Showing rationale for permission.")
      groupPermissionHint.visibility = View.VISIBLE
      requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE)
    } else {
      Log.d(TAG, "No need to show rationale. Request permission.")
      requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE)
    }
  }

  private fun loadNavHost() {
    // hide hints, show navigation
    groupPermissionHint.visibility = View.GONE
    navigation_bar.visibility = View.VISIBLE

    // enable navigation
    val finalHost = NavHostFragment.create(R.navigation.library_navigation)
    supportFragmentManager.beginTransaction()
      .replace(R.id.library_nav_host, finalHost)
      .setPrimaryNavigationFragment(finalHost)
      .runOnCommit {
        val navController = findNavController(R.id.library_nav_host)

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
      .commit()
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
          loadNavHost()
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
