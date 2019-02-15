package hu.mrolcsi.android.lyricsplayer.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.library.albums.AlbumsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.library.songs.SongsFragmentArgs
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_library)
  }

  override fun onStart() {
    super.onStart()

    init()
  }

  private fun init() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
      != PackageManager.PERMISSION_GRANTED
    ) {
      // Permission is not granted
      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          this,
          Manifest.permission.READ_EXTERNAL_STORAGE
        )
      ) {
        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.
        with(AlertDialog.Builder(this)) {
          setTitle("Read External Storage")
          setMessage("I can haz external storage? :(")
          setPositiveButton(android.R.string.ok) { _, _ ->
            init()
          }
          show()
        }
      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(
          this,
          arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
          PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE
        )

        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
        // app-defined int constant. The callback method gets the
        // result of the request.
      }
    } else {
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
              R.id.navigation_albumsByArtist -> {
                if (arguments != null) {
                  val args = AlbumsFragmentArgs.fromBundle(arguments)
                  supportActionBar?.subtitle = "by ${args.artist}"  // TODO: i18n
                } else {
                  supportActionBar?.subtitle = null
                }
              }
              R.id.navigation_songsFromAlbum -> {
                if (arguments != null) {
                  val args = SongsFragmentArgs.fromBundle(arguments)
                  supportActionBar?.subtitle = "from ${args.albumTitle}"  // TODO: i18n
                } else {
                  supportActionBar?.subtitle = null
                }
              }
            }
          }
        }
        .commit()
    }
  }


  override fun onSupportNavigateUp(): Boolean {
    return findNavController(R.id.library_nav_host).navigateUp()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    when (requestCode) {
      PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE -> {
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          // permission was granted, yay! Do the
          // contacts-related task you need to do.
          init()
        } else {
          // permission denied, boo! Disable the
          // functionality that depends on this permission.
          init()  // ask again
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
    const val PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE = 9514
  }
}
