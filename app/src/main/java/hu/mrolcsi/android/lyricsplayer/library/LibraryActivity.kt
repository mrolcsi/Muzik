package hu.mrolcsi.android.lyricsplayer.library

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import hu.mrolcsi.android.lyricsplayer.R
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity() {

  private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
    when (item.itemId) {
      R.id.navigation_artists -> {
        // TODO: navigate to ArtistsFragment
        return@OnNavigationItemSelectedListener true
      }
      R.id.navigation_albums -> {
        // TODO: navigate to AlbumsFragment
        return@OnNavigationItemSelectedListener true
      }
      R.id.navigation_songs -> {
        // TODO: navigate to SongsFragment
        return@OnNavigationItemSelectedListener true
      }
    }
    false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_library)

    navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
  }
}
