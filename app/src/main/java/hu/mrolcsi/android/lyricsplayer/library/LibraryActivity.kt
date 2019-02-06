package hu.mrolcsi.android.lyricsplayer.library

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity() {

  private lateinit var mMediaBrowser: MediaBrowserCompat

  //region CALLBACKS

  private val mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    // TODO
  }

  private val mArtistsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
      mArtistAdapter.submitList(children)
    }
  }

  //endregion

  //region LIFECYCLE

  private val mArtistAdapter = ArtistAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_library)
    supportActionBar?.setDisplayHomeAsUpEnabled(false)

    // Create MediaBrowserServiceCompat
    mMediaBrowser = MediaBrowserCompat(
      this,
      ComponentName(this, LPBrowserService::class.java),
      mConnectionCallbacks,
      null // optional Bundle
    )

    rvLibrary.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    rvLibrary.adapter = mArtistAdapter
  }

  override fun onStart() {
    super.onStart()

    // check for permission first
    init()
  }

  override fun onStop() {
    super.onStop()

    mMediaBrowser.unsubscribe(LPBrowserService.MEDIA_ARTISTS_ID)

    mMediaBrowser.disconnect()
  }

  //endregion

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
      mMediaBrowser.subscribe(LPBrowserService.MEDIA_ARTISTS_ID, mArtistsSubscription)

      mMediaBrowser.connect()
    }
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
