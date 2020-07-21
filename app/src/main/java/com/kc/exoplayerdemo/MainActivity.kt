package com.kc.exoplayerdemo

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MainActivity : AppCompatActivity() {
    private lateinit var playerView: PlayerView
    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow: Int = 0
    private var playbackPosition: Long = 0
    private val userAgent = "user-agent"
    private val licenseUrlWorking =
        "https://proxy.uat.widevine.com/proxy?video_id=GTS_SW_SECURE_DECODE&provider=widevine_test"
    private val licenseUrlSample = "https://content.uplynk.com/wv"

    private var playbackStateListener = PlaybackStateListener()
    private var TAG = MainActivity::class.qualifiedName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playerView = findViewById(R.id.video_view)
    }

    private fun initializePlayer() {
        val drmCallback =
            HttpMediaDrmCallback(licenseUrlWorking, DefaultHttpDataSourceFactory(userAgent))
        val drmSessionManager = DefaultDrmSessionManager(
            C.WIDEVINE_UUID,
            FrameworkMediaDrm.newInstance(C.WIDEVINE_UUID), drmCallback, null, false, 1
        )

        val trackSelector = DefaultTrackSelector()
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
        player = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultRenderersFactory(this, drmSessionManager),
            trackSelector
        )

        playerView.player = player
        val uri = Uri.parse(getString(R.string.working_dash_url))
        val mediaSource = buildDrmDashMediaSource(uri)
        player!!.playWhenReady = playWhenReady
        player!!.seekTo(currentWindow, playbackPosition)
        player!!.addListener(playbackStateListener)
        player!!.prepare(mediaSource, false, false)

        player!!.addListener(
            object : Player.EventListener {
                override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {
                    val mManifest = player!!.currentManifest
                    if (mManifest != null) {
                        val dashManifest = mManifest as DashManifest
                        // Do something with the manifest.
                        Log.d("TAG", "DashManifest: $dashManifest")
                    }
                }
            })
    }

    private fun buildDrmDashMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, "exoplayer-codelab")
        val dFactory = DefaultDashChunkSource.Factory(dataSourceFactory)
        val dashFactory = DashMediaSource.Factory(dFactory, dataSourceFactory)
        return dashFactory.createMediaSource(uri)
    }

    private fun hideSystemUI() {
        playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    override fun onStart() {
        super.onStart()
        // After API 24 multi window is supported to we can take advantage
        if (Util.SDK_INT > 24) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24 && player == null) {
            hideSystemUI()
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        if (player != null) {
            playWhenReady = player!!.playWhenReady
            playbackPosition = player!!.currentPosition
            currentWindow = player!!.currentWindowIndex
            player!!.removeListener(playbackStateListener)
            player!!.release()
            player = null
        }
    }

    class PlaybackStateListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            val stateString: String
            stateString = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d("TAG", "changed state to $stateString playWhenReady: $playWhenReady")
        }
    }
}