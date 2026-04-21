package com.example.anotheriptv.presentation.player

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ActivityPlayerBinding
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    private val hideControlsRunnable = Runnable { hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val channelName = intent.getStringExtra("channelName") ?: "Channel"
        val streamUrl   = intent.getStringExtra("streamUrl")   ?: return

        binding.tvChannelName.text = channelName

        setupPlayer(streamUrl)
        setupControls()
        scheduleHideControls()
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor { chain ->
                // VLC User-Agent để server Xtream không reset connection
                val request = chain.request().newBuilder()
                    .header("User-Agent", "VLC/3.0.0 LibVLC/3.0.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private fun setupPlayer(url: String) {
        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            OkHttpDataSource.Factory(buildOkHttpClient())
        )

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().also { exo ->
                binding.playerView.player = exo
                exo.setMediaItem(MediaItem.fromUri(url))
                exo.prepare()
                exo.play()

                exo.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        binding.btnPlayPause.setImageResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        )
                    }
                })
            }

        binding.playerView.postDelayed(object : Runnable {
            override fun run() {
                player?.let { p ->
                    val duration = p.duration.coerceAtLeast(0)
                    val position = p.currentPosition.coerceAtLeast(0)
                    if (duration > 0) {
                        binding.seekBar.progress = ((position * 100) / duration).toInt()
                    }
                    binding.tvCurrentTime.text = formatTime(position)
                    binding.tvTotalTime.text   = formatTime(duration)
                }
                binding.playerView.postDelayed(this, 500)
            }
        }, 500)
    }

    private fun setupControls() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPlayPause.setOnClickListener {
            player?.let { if (it.isPlaying) it.pause() else it.play() }
            scheduleHideControls()
        }

        binding.btnFavorite.setOnClickListener {
            // TODO: toggle favorite
            scheduleHideControls()
        }

        binding.btnInfo.setOnClickListener {
            // TODO: show info
            scheduleHideControls()
        }

        binding.btnSettings.setOnClickListener {
            // TODO: show quality settings
            scheduleHideControls()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player?.duration ?: 0
                    player?.seekTo((duration * progress) / 100)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { scheduleHideControls() }
        })

        binding.playerView.setOnClickListener {
            if (binding.layoutTopControls.visibility == View.VISIBLE) {
                hideControls()
            } else {
                showControls()
                scheduleHideControls()
            }
        }
    }

    private fun showControls() {
        binding.layoutTopControls.visibility  = View.VISIBLE
        binding.layoutBottomControls.visibility = View.VISIBLE
        binding.btnPlayPause.visibility        = View.VISIBLE
    }

    private fun hideControls() {
        binding.layoutTopControls.visibility  = View.GONE
        binding.layoutBottomControls.visibility = View.GONE
        binding.btnPlayPause.visibility        = View.GONE
    }

    private fun scheduleHideControls() {
        binding.playerView.removeCallbacks(hideControlsRunnable)
        binding.playerView.postDelayed(hideControlsRunnable, 3000)
    }

    private fun formatTime(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}