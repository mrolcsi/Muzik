package hu.mrolcsi.muzik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.NavHostFragment
import hu.mrolcsi.muzik.extensions.isPermissionGranted
import hu.mrolcsi.muzik.extensions.requestPermission
import hu.mrolcsi.muzik.extensions.shouldShowPermissionRationale
import hu.mrolcsi.muzik.player.PlayerViewModel
import kotlinx.android.synthetic.main.content_permission.*

class MainActivity : AppCompatActivity() {

  private lateinit var mPlayerModel: PlayerViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.FluxTheme)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    if (savedInstanceState == null) {
      // Initial loading
      if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) || !isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        Log.d(LOG_TAG, "Missing permission. Request from user.")
        requestStoragePermission()
      } else {
        Log.d(LOG_TAG, "Permission already granted. Loading Library.")
        createNavHost()
      }
    }

    mPlayerModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
      mediaController.observe(this@MainActivity, Observer { controller ->
        // Set mediaController to the Activity
        MediaControllerCompat.setMediaController(this@MainActivity, controller)
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

  @Suppress("UNUSED_PARAMETER")
  fun requestStoragePermission(view: View? = null) {
    if (shouldShowPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      Log.d(LOG_TAG, "Showing rationale for permission.")

      groupPermissionHint.visibility = View.VISIBLE

      requestPermission(
        PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
    } else {
      Log.d(LOG_TAG, "No need to show rationale. Request permission.")
      requestPermission(
        PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
    }
  }

  private fun createNavHost() {
    Log.d(LOG_TAG, "Creating navigation host.")

    // enable navigation
    val finalHost = NavHostFragment.create(R.navigation.main_navigation)
    supportFragmentManager.beginTransaction()
      .replace(R.id.main_nav_host, finalHost)
      .setPrimaryNavigationFragment(finalHost)
      .runOnCommit {
        groupPermissionHint.visibility = View.GONE
      }.commit()
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
    private const val LOG_TAG = "MainActivity"

    private const val PERMISSION_REQUEST_CODE_EXTERNAL_STORAGE = 9514
  }
}