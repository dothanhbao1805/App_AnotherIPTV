package com.example.anotheriptv.presentation.player

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ActivityPlayerBinding
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    private val hideControlsRunnable = Runnable { hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val channelName = intent.getStringExtra("channelName") ?: "Channel"
        val streamUrl = intent.getStringExtra("streamUrl") ?: return

        binding.tvChannelName.text = channelName

        setupPlayer(streamUrl)
        setupControls()
        scheduleHideControls()
    }

    private fun setupPlayer(url: String) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            val mediaItem = MediaItem.fromUri(url)
            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.play()

            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    binding.btnPlayPause.setImageResource(
                        if (isPlaying) R.drawable.ic_pause
                        else R.drawable.ic_play
                    )
                }
            })
        }

        // Update seekbar mỗi 500ms
        binding.playerView.postDelayed(object : Runnable {
            override fun run() {
                player?.let { p ->
                    val duration = p.duration.coerceAtLeast(0)
                    val position = p.currentPosition.coerceAtLeast(0)
                    if (duration > 0) {
                        binding.seekBar.progress = ((position * 100) / duration).toInt()
                    }
                    binding.tvCurrentTime.text = formatTime(position)
                    binding.tvTotalTime.text = formatTime(duration)
                }
                binding.playerView.postDelayed(this, 500)
            }
        }, 500)
    }

    private fun setupControls() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPlayPause.setOnClickListener {
            player?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
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
                    val seekTo = (duration * progress) / 100
                    player?.seekTo(seekTo)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { scheduleHideControls() }
        })

        // Tap để show/hide controls
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
        binding.layoutTopControls.visibility = View.VISIBLE
        binding.layoutBottomControls.visibility = View.VISIBLE
        binding.btnPlayPause.visibility = View.VISIBLE
    }

    private fun hideControls() {
        binding.layoutTopControls.visibility = View.GONE
        binding.layoutBottomControls.visibility = View.GONE
        binding.btnPlayPause.visibility = View.GONE
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