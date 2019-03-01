package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.extensions.media.build
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class PlayerHolder(context: Context, session: MediaSessionCompat) {

  private val mPlayer = ExoPlayerFactory.newSimpleInstance(
    context,
    AudioOnlyRenderersFactory(context),
    DefaultTrackSelector()
  )

  private val mQueue = ConcatenatingMediaSource()

  private val mPlaybackPreparer = object : MediaSessionConnector.PlaybackPreparer {
    override fun getCommands(): Array<String> = arrayOf()

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {}

    override fun getSupportedPrepareActions(): Long = MediaSessionConnector.PlaybackPreparer.ACTIONS

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
      // Assuming mediaId is a path
      onPrepareFromUri(Uri.fromFile(File(mediaId)), extras)
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
      // Reset media player before loading
      Log.v(LOG_TAG, "Loading media into Player: $uri")

      // Create MediaSource from uri
      val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, BuildConfig.APPLICATION_ID))
      val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
      mPlayer.prepare(mediaSource)

      // Load metadata
      MediaMetadataRetriever().apply {
        setDataSource(context, uri)
        session.setMetadata(this.build())
      }
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
      TODO("Do search, create uri, prepareFromUri")
    }

    override fun onPrepare() {
      TODO()
    }
  }

  private val mPlaybackController = object : DefaultPlaybackController() {
    // TODO: override functions as needed
  }

  private val mQueueNavigator = object : TimelineQueueNavigator(session) {
    override fun getMediaDescription(player: Player?, windowIndex: Int): MediaDescriptionCompat {
      // TODO: return mQueue[windowIndex].description?
      return session.controller.metadata.description
    }
  }

  private val mQueueDataAdapter = object : TimelineQueueEditor.QueueDataAdapter {
    override fun add(position: Int, description: MediaDescriptionCompat?) {}

    override fun remove(position: Int) {}

    override fun move(from: Int, to: Int) {}
  }

  private val mMediaSourceFactory = TimelineQueueEditor.MediaSourceFactory { description ->
    // Return a MediaSource from the MediaDescription
    val uri = description.mediaUri ?: Uri.fromFile(File(description.mediaId))
    val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, BuildConfig.APPLICATION_ID))
    ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
  }

  // Player state polling
  private val mUpdaterEnabled = AtomicBoolean(false)
  private val mUpdateHandler = Handler()
  private val mUpdateRunnable = object : Runnable {
    override fun run() {

      // TODO: update session with current positions

      if (mUpdaterEnabled.get()) {
        mUpdateHandler.postDelayed(this, UPDATE_FREQUENCY)
      }
    }
  }

  init {
    // Connect this holder to the session
    MediaSessionConnector(session, mPlaybackController).also {
      it.setPlayer(mPlayer, mPlaybackPreparer)
      it.setQueueNavigator(mQueueNavigator)
      it.setQueueEditor(
        TimelineQueueEditor(
          session.controller,
          mQueue,
          mQueueDataAdapter,
          mMediaSourceFactory
        )
      )
    }
  }

  companion object {
    private const val LOG_TAG = "PlayerHolder"

    private const val UPDATE_FREQUENCY: Long = 500  // in milliseconds
  }

}