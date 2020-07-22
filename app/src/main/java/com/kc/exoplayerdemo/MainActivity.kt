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

    // Change this
    private val isDrm = true
    private val licenseUrl = licenseUrlSample

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playerView = findViewById(R.id.video_view)
    }

    private fun initializePlayer(isDrm: Boolean) {
        val trackSelector = DefaultTrackSelector()
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
        trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredTextLanguage("en"))

        if (isDrm) {
            val drmCallback =
                HttpMediaDrmCallback(licenseUrl, DefaultHttpDataSourceFactory(userAgent))
            val drmSessionManager = DefaultDrmSessionManager(
                C.WIDEVINE_UUID,
                FrameworkMediaDrm.newInstance(C.WIDEVINE_UUID), drmCallback, null, true, 1
            )
            player = ExoPlayerFactory.newSimpleInstance(
                this,
                DefaultRenderersFactory(this, drmSessionManager),
                trackSelector
            )
        } else {
            player = ExoPlayerFactory.newSimpleInstance(
                this,
                trackSelector
            )
        }

        playerView.player = player




        /**
         * Change this
         */
        val uri = Uri.parse(getString(R.string.eros_dash_url))
//        val uri = Uri.parse("https://content.uplynk.com/a0c04727eda44eca8bc116e654aa1439.mpd?drm_policy_name=TEST001&exp=1596282416&cid=a0c04727eda44eca8bc116e654aa1439&rn=3256526194&tc=1&ct=a&sig=3b4cda3cc117dd09f257ff827c34c9c105735fed78b96a80aa4fa2d0763b2313")
        val mediaSource = buildDrmDashMediaSource(uri)

        player?.let {
            it.playWhenReady = playWhenReady
            it.seekTo(currentWindow, playbackPosition)
            it.addListener(playbackStateListener)
            it.prepare(mediaSource, false, false)
            /*it.addListener(
                object : Player.EventListener {
                    override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {
                        val mManifest = player!!.currentManifest
                        if (mManifest != null) {
                            val dashManifest = mManifest as DashManifest
                            // Do something with the manifest.
                            Log.d("TAG", "DashManifest: $dashManifest")
                        }
                    }
                })*/
        }
    }

    private fun buildDrmDashMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, userAgent)
        //val dFactory = DefaultDashChunkSource.Factory(dataSourceFactory)
        val dashFactory = DashMediaSource.Factory( dataSourceFactory)
        return dashFactory.createMediaSource(uri)
    }

    @Suppress("DEPRECATION")
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
            initializePlayer(isDrm)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24 && player == null) {
            hideSystemUI()
            initializePlayer(isDrm)
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
        player?.let { it ->
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            it.removeListener(playbackStateListener)
            it.release()
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