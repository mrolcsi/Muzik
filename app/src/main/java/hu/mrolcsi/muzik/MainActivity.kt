package hu.mrolcsi.muzik

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.content_permission.*

class MainActivity : AppCompatActivity() {

  private val permissions = RxPermissions(this)

  private val requiredPermissions = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
  )

  private var disposable: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.FluxTheme)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    if (savedInstanceState == null) {
      // Initial loading
      if (requiredPermissions.all { permissions.isGranted(it) }) {
        Log.d(LOG_TAG, "Permission already granted. Loading Library.")
        createNavHost()

      } else {
        Log.d(LOG_TAG, "Missing permission. Request from user.")
        requestStoragePermission()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    disposable?.dispose()
  }

  @Suppress("UNUSED_PARAMETER")
  fun requestStoragePermission(view: View? = null) {
    disposable = permissions.requestEach(*requiredPermissions)
      .subscribeBy { permission ->
        when {
          permission.granted -> createNavHost()
          else -> groupPermissionHint.visibility = View.VISIBLE
        }
    }
  }

  private fun createNavHost() {
    Log.d(LOG_TAG, "Creating navigation host.")

    // enable navigation
    val finalHost = NavHostFragment.create(R.navigation.main_navigation)
    supportFragmentManager.beginTransaction()
      .replace(R.id.main_nav_host, finalHost)
      .setPrimaryNavigationFragment(finalHost)
      .runOnCommit { groupPermissionHint.visibility = View.GONE }
      .commit()
  }

  companion object {
    private const val LOG_TAG = "MainActivity"
  }
}