package com.example.anotheriptv.presentation.player.xstream

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ActivityPlayerLiveXstreamBinding
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.presentation.player.xstream.Adapter.CategoryAdapter
import com.example.anotheriptv.presentation.player.xstream.Adapter.ChannelListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@OptIn(UnstableApi::class)
class PlayerLiveXstreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerLiveXstreamBinding
    private var player: ExoPlayer? = null
    private var isFavorite = false
    private var isInfoVisible = false

    private var channelName: String = ""
    private var streamUrl: String = ""

    private var isListVisible = false
    private var isShowingChannels = false

    private var playlistId: Long = -1L
    private var contentType: String = "live"

    private var currentCategoryName: String = ""
    private var allCategoriesWithChannels: List<CategoryWithChannels> = emptyList()


    private val hideControlsRunnable = Runnable { hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerLiveXstreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelName = intent.getStringExtra("channelName") ?: "Channel"
        streamUrl   = intent.getStringExtra("streamUrl")   ?: return
        playlistId  = intent.getLongExtra("playlistId", -1L)
        contentType = intent.getStringExtra("contentType") ?: "live"

        binding.tvChannelName.text = channelName

        setupPlayer(streamUrl)
        checkFavoriteStatus(streamUrl)
        setupControls(streamUrl)
        setupListPanel()  // ← THIẾU DÒNG NÀY
    }

    // ── OkHttp (trust all SSL) ────────────────────────────────────────────────

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
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "VLC/3.0.0 LibVLC/3.0.0")
                        .build()
                )
            }
            .build()
    }

    // ── Player setup ──────────────────────────────────────────────────────────

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
                binding.progressBuffering.visibility = View.VISIBLE

                exo.addListener(object : Player.Listener {

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (player?.playbackState == Player.STATE_BUFFERING) return

                        if (isPlaying) showPauseBtn() else showPlayBtn()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                // Đang buffer → chỉ hiện loading
                                binding.progressBuffering.visibility = View.VISIBLE
                                binding.btnPlay.visibility           = View.GONE
                                binding.btnPause.visibility          = View.GONE
                            }
                            Player.STATE_READY -> {
                                // Sẵn sàng → ẩn loading, hiện đúng nút
                                binding.progressBuffering.visibility = View.GONE
                                if (player?.isPlaying == true) showPauseBtn() else showPlayBtn()
                            }
                            Player.STATE_ENDED,
                            Player.STATE_IDLE -> {
                                binding.progressBuffering.visibility = View.GONE
                                binding.btnPlay.visibility           = View.GONE
                                binding.btnPause.visibility          = View.GONE
                            }
                        }
                    }
                })
            }

        // Cập nhật seekbar + time mỗi 500ms
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

    // ── Controls setup ────────────────────────────────────────────────────────

    private fun setupControls(url: String) {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPause.setOnClickListener {
            player?.pause()
            showPlayBtn()
            scheduleHideControls()
        }

        binding.btnPlay.setOnClickListener {
            player?.play()
            showPauseBtn()
            scheduleHideControls()
        }

        binding.btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteIcon()

            lifecycleScope.launch(Dispatchers.IO) {
                val container = (application as MyApp).container
                container.channelDao.updateFavoriteStatus(url, isFavorite)
            }
            scheduleHideControls()
        }

        binding.btnInfo.setOnClickListener {
            toggleInfoPanel()
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
            if (isInfoVisible) {
                hideInfoPanel()
                return@setOnClickListener
            }
            if (binding.layoutTopControls.visibility == View.VISIBLE) {
                hideControls()
            } else {
                showControls()
                scheduleHideControls()
            }
        }

    }

    private fun setupListPanel() {
        binding.layoutListPanel.btnCloseList.setOnClickListener {
            hideListPanel()
        }

        binding.layoutListPanel.btnListBack.setOnClickListener {
            // Quay lại màn Categories
            showCategories()
        }

        binding.btnList.setOnClickListener {
            toggleListPanel()
        }
    }

    private fun toggleListPanel() {
        if (isListVisible) hideListPanel() else showListPanel()
    }

    private fun showListPanel() {
        isListVisible = true
        binding.layoutListPanel.root.visibility = View.VISIBLE

        // Nếu chưa có data thì load, sau đó show channel list luôn
        if (allCategoriesWithChannels.isEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val container = (application as MyApp).container
                allCategoriesWithChannels = container.channelRepository
                    .getAllCategoriesWithChannels(playlistId, contentType)

                // Tìm category của kênh đang phát
                currentCategoryName = allCategoriesWithChannels
                    .find { group -> group.channels.any { it.url == streamUrl } }
                    ?.categoryName ?: allCategoriesWithChannels.firstOrNull()?.categoryName ?: ""

                withContext(Dispatchers.Main) {
                    showChannelsOfCategory(currentCategoryName)
                }
            }
        } else {
            // Đã có data rồi thì show luôn
            if (currentCategoryName.isEmpty()) {
                currentCategoryName = allCategoriesWithChannels
                    .find { group -> group.channels.any { it.url == streamUrl } }
                    ?.categoryName ?: allCategoriesWithChannels.firstOrNull()?.categoryName ?: ""
            }
            showChannelsOfCategory(currentCategoryName)
        }
    }

    private fun hideListPanel() {
        isListVisible = false
        binding.layoutListPanel.root.visibility = View.GONE
    }

    private fun showCategories() {
        isShowingChannels = false
        binding.layoutListPanel.tvListTitle.text = "Categories"
        binding.layoutListPanel.btnListBack.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            val currentCategoryNameLocal = allCategoriesWithChannels
                .find { group -> group.channels.any { it.url == streamUrl } }
                ?.categoryName ?: ""

            val categoryItems = allCategoriesWithChannels.map {
                CategoryAdapter.CategoryItem(
                    name         = it.categoryName,
                    channelCount = it.channels.size
                )
            }

            // Tìm index của category hiện tại
            val currentIndex = categoryItems.indexOfFirst { it.name == currentCategoryNameLocal }

            withContext(Dispatchers.Main) {
                binding.layoutListPanel.rvList.layoutManager =
                    LinearLayoutManager(this@PlayerLiveXstreamActivity)
                binding.layoutListPanel.rvList.adapter = CategoryAdapter(
                    categoryItems, currentCategoryNameLocal
                ) { selected ->
                    showChannelsOfCategory(selected.name)
                }

                if (currentIndex >= 0) {
                    binding.layoutListPanel.rvList.scrollToPosition(currentIndex)
                }
            }
        }
    }

    private fun showChannelsOfCategory(categoryName: String) {
        isShowingChannels = true
        binding.layoutListPanel.tvListTitle.text = categoryName
        binding.layoutListPanel.btnListBack.visibility = View.VISIBLE

        val channels = allCategoriesWithChannels
            .find { it.categoryName == categoryName }
            ?.channels ?: emptyList()

        val existingAdapter = binding.layoutListPanel.rvList.adapter

        if (existingAdapter is ChannelListAdapter &&
            binding.layoutListPanel.tvListTitle.text == categoryName) {
            // Cùng category → chỉ update url highlight, không tạo lại adapter
            existingAdapter.updateCurrentUrl(streamUrl)
        } else {
            // Khác category → tạo adapter mới
            binding.layoutListPanel.rvList.layoutManager =
                LinearLayoutManager(this@PlayerLiveXstreamActivity)
            binding.layoutListPanel.rvList.adapter = ChannelListAdapter(
                channels, streamUrl
            ) { selected ->
                switchChannel(selected.name, selected.url ?: "")
            }
        }

        val currentIndex = channels.indexOfFirst { it.url == streamUrl }
        if (currentIndex >= 0) {
            binding.layoutListPanel.rvList.scrollToPosition(currentIndex)
        }
    }

    private fun switchChannel(name: String, url: String) {
        channelName = name
        streamUrl   = url
        binding.tvChannelName.text = channelName

        // Cập nhật category hiện tại theo kênh mới
        currentCategoryName = allCategoriesWithChannels
            .find { group -> group.channels.any { it.url == url } }
            ?.categoryName ?: currentCategoryName

        player?.let { exo ->
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.play()
        }

        binding.progressBuffering.visibility = View.VISIBLE
        binding.btnPlay.visibility  = View.GONE
        binding.btnPause.visibility = View.GONE

        // Refresh adapter để update highlight kênh đang phát
        showChannelsOfCategory(currentCategoryName)

        checkFavoriteStatus(url)
    }


    private fun checkFavoriteStatus(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val container = (application as MyApp).container
            // Tìm channel trong DB qua URL
            val channel = container.channelRepository.getChannelByUrl(url)
            isFavorite = channel?.isFavorite ?: false

            withContext(Dispatchers.Main) {
                updateFavoriteIcon()
            }
        }
    }

    private fun updateFavoriteIcon() {
        val icon = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        binding.btnFavorite.setImageResource(icon)
    }

    // ── Visibility helpers ────────────────────────────────────────────────────

    private fun showPlayBtn() {
        if (binding.layoutTopControls.visibility == View.VISIBLE) {
            binding.btnPlay.visibility  = View.VISIBLE
            binding.btnPause.visibility = View.GONE
        }
    }

    private fun showPauseBtn() {
        if (binding.layoutTopControls.visibility == View.VISIBLE) {
            binding.btnPause.visibility = View.VISIBLE
            binding.btnPlay.visibility  = View.GONE
        }
    }

    private fun showControls() {
        binding.layoutTopControls.visibility    = View.VISIBLE
        binding.layoutBottomControls.visibility = View.VISIBLE

        if (binding.progressBuffering.visibility == View.VISIBLE) {
            binding.btnPlay.visibility  = View.GONE
            binding.btnPause.visibility = View.GONE
            return
        }

        if (player?.isPlaying == true) {
            binding.btnPause.visibility = View.VISIBLE
            binding.btnPlay.visibility  = View.GONE
        } else {
            binding.btnPlay.visibility  = View.VISIBLE
            binding.btnPause.visibility = View.GONE
        }
    }

    private fun hideControls() {
        binding.layoutTopControls.visibility    = View.GONE
        binding.layoutBottomControls.visibility = View.GONE
        binding.btnPlay.visibility              = View.GONE
        binding.btnPause.visibility             = View.GONE
    }

    private fun scheduleHideControls() {
        binding.playerView.removeCallbacks(hideControlsRunnable)
        binding.playerView.postDelayed(hideControlsRunnable, 3000)
    }

    // ──────────────────────────────────────────────────────
    private fun formatTime(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun toggleInfoPanel() {
        if (isInfoVisible) hideInfoPanel() else showInfoPanel()
    }

    private fun showInfoPanel() {
        isInfoVisible = true

        binding.layoutInfoPanel.tvInfoName.text        = channelName
        binding.layoutInfoPanel.tvInfoContentType.text = "Live Stream"
        binding.layoutInfoPanel.tvInfoEpgId.text       = intent.getStringExtra("epgId") ?: "—"
        binding.layoutInfoPanel.tvInfoStreamId.text    = intent.getStringExtra("streamId") ?: "—"
        binding.layoutInfoPanel.tvInfoUrl.text         = streamUrl

        binding.layoutInfoPanel.root.visibility = View.VISIBLE  // ← dùng .root

        binding.layoutInfoPanel.btnCopyStreamId.setOnClickListener {
            copyToClipboard("Stream ID", binding.layoutInfoPanel.tvInfoStreamId.text.toString())
        }
        binding.layoutInfoPanel.btnCopyUrl.setOnClickListener {
            copyToClipboard("URL", streamUrl)
        }
        binding.layoutInfoPanel.btnCloseInfo.setOnClickListener {
            hideInfoPanel()
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
        Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
    }

    private fun hideInfoPanel() {
        isInfoVisible = false
        binding.layoutInfoPanel.root.visibility = View.GONE
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