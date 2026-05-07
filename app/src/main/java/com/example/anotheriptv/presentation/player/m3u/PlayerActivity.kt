package com.example.anotheriptv.presentation.player.m3u

import android.content.Context
import android.content.Intent
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
import com.example.anotheriptv.databinding.ActivityPlayerBinding
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.presentation.player.xstream.Adapter.CategoryAdapter
import com.example.anotheriptv.presentation.player.xstream.Adapter.ChannelListAdapter
import com.example.anotheriptv.service.MediaPlaybackService
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
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var isFavorite = false
    private var isInfoVisible = false

    private var channelName: String = ""
    private var streamUrl: String = ""

    private var isListVisible = false
    private var isShowingChannels = false

    private var playlistId: Long = -1L
    private var contentType: String = "LIVE"

    private var currentCategoryName: String = ""
    private var allCategoriesWithChannels: List<CategoryWithChannels> = emptyList()

    private var videoTrackAuto     = true
    private var audioTrackAuto     = true
    private var subtitleTrackAuto  = true
    private var isSettingVisible = false

    private var seekBackwardAmount = 0
    private var seekForwardAmount = 0
    private val seekHideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hideSeekBackwardRunnable = Runnable {
        binding.layoutSeekBackward.visibility = View.GONE
        seekBackwardAmount = 0
    }
    private val hideSeekForwardRunnable = Runnable {
        binding.layoutSeekForward.visibility = View.GONE
        seekForwardAmount = 0
    }

    private val hideControlsRunnable = Runnable { hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelName = intent.getStringExtra("channelName") ?: "Channel"
        streamUrl   = intent.getStringExtra("streamUrl")   ?: return
        playlistId  = intent.getLongExtra("playlistId", -1L)
        contentType = intent.getStringExtra("contentType") ?: "live"

        binding.tvChannelName.text = channelName

        setupPlayer(streamUrl)
        checkFavoriteStatus(streamUrl)
        setupControls(streamUrl)
        setupSettingPanel()
        setupTouchAndGestures()
        setupListPanel()
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
            toggleSettingPanel()
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
                    LinearLayoutManager(this@PlayerActivity)
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
                LinearLayoutManager(this@PlayerActivity)
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
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
            binding.btnFavorite.imageTintList = null
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            binding.btnFavorite.imageTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.WHITE
            )
        }
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


    private fun setupSettingPanel() {
        binding.layoutSettingPanel.btnCloseSetting.setOnClickListener {
            hideSettingPanel()
        }

        binding.layoutSettingPanel.btnVideoAuto.setOnClickListener {
            videoTrackAuto = true
            updateVideoTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, false)
                .build()
        }

        binding.layoutSettingPanel.btnVideoDisabled.setOnClickListener {
            videoTrackAuto = false
            updateVideoTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, true)
                .build()
        }

        binding.layoutSettingPanel.btnAudioAuto.setOnClickListener {
            audioTrackAuto = true
            updateAudioTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, false)
                .build()
        }

        binding.layoutSettingPanel.btnAudioDisabled.setOnClickListener {
            audioTrackAuto = false
            updateAudioTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                .build()
        }

        binding.layoutSettingPanel.btnSubtitleAuto.setOnClickListener {
            subtitleTrackAuto = true
            updateSubtitleTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                .build()
        }

        binding.layoutSettingPanel.btnSubtitleDisabled.setOnClickListener {
            subtitleTrackAuto = false
            updateSubtitleTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                .build()
        }
    }

    private fun updateVideoTrackUI() {
        binding.layoutSettingPanel.btnVideoAuto.setBackgroundResource(
            if (videoTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option
        )
        binding.layoutSettingPanel.ivVideoAutoCheck.visibility =
            if (videoTrackAuto) View.VISIBLE else View.GONE
        binding.layoutSettingPanel.btnVideoDisabled.setBackgroundResource(
            if (!videoTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option
        )
        binding.layoutSettingPanel.ivVideoDisabledCheck.visibility =
            if (!videoTrackAuto) View.VISIBLE else View.GONE
    }

    private fun updateAudioTrackUI() {
        binding.layoutSettingPanel.btnAudioAuto.setBackgroundResource(
            if (audioTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option
        )
        binding.layoutSettingPanel.ivAudioAutoCheck.visibility =
            if (audioTrackAuto) View.VISIBLE else View.GONE
        binding.layoutSettingPanel.btnAudioDisabled.setBackgroundResource(
            if (!audioTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option
        )
        binding.layoutSettingPanel.ivAudioDisabledCheck.visibility =
            if (!audioTrackAuto) View.VISIBLE else View.GONE
    }

    private fun updateSubtitleTrackUI() {
        binding.layoutSettingPanel.btnSubtitleAuto.setBackgroundResource(
            if (subtitleTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option
        )
        binding.layoutSettingPanel.ivSubtitleAutoCheck.visibility =
            if (subtitleTrackAuto) View.VISIBLE else View.GONE
        binding.layoutSettingPanel.btnSubtitleDisabled.setBackgroundResource(
            if (!subtitleTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option
        )
        binding.layoutSettingPanel.ivSubtitleDisabledCheck.visibility =
            if (!subtitleTrackAuto) View.VISIBLE else View.GONE
    }

    private fun toggleSettingPanel() {
        if (isSettingVisible) hideSettingPanel() else showSettingPanel()
    }

    private fun showSettingPanel() {
        isSettingVisible = true
        binding.layoutSettingPanel.root.visibility = View.VISIBLE
    }

    private fun hideSettingPanel() {
        isSettingVisible = false
        binding.layoutSettingPanel.root.visibility = View.GONE
    }

    private fun isContinuePlayingEnabled(): Boolean {
        return getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("continue_playing_bg", false)
    }

    override fun onPause() {
        super.onPause()
        if (isContinuePlayingEnabled()) {
            val serviceIntent = Intent(this, MediaPlaybackService::class.java).apply {
                putExtra("channelName", channelName)
            }
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            player?.pause()
        }
    }

    private fun setupTouchAndGestures() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val layoutSpeedIndicator = binding.layoutSpeedIndicator

        // 1. Xử lý chạm giữ (Long Press) để tua 2.0x
        binding.playerView.setOnLongClickListener {
            if (prefs.getBoolean("speed_up_long_press", true)) {
                player?.setPlaybackSpeed(2.0f)
                layoutSpeedIndicator.visibility = View.VISIBLE
                hideControls()
            }
            true
        }

        // 2. Khởi tạo GestureDetector để bắt Double Tap
        val gestureDetector = android.view.GestureDetector(
            this,
            object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                    if (!prefs.getBoolean("seek_double_tap", true)) return false

                    val screenWidth = binding.playerView.width
                    val tapX = e.x

                    if (tapX < screenWidth / 2) {
                        // Bên trái → tua lùi
                        seekBackwardAmount += 10
                        val newPos = (player?.currentPosition ?: 0) - 10_000
                        player?.seekTo(newPos.coerceAtLeast(0))

                        seekHideHandler.removeCallbacks(hideSeekBackwardRunnable)
                        binding.layoutSeekBackward.visibility = View.VISIBLE
                        binding.tvSeekBackwardSeconds.text = "$seekBackwardAmount seconds"
                        seekHideHandler.postDelayed(hideSeekBackwardRunnable, 800)
                    } else {
                        // Bên phải → tua tới
                        seekForwardAmount += 10
                        val newPos = (player?.currentPosition ?: 0) + 10_000
                        val duration = player?.duration ?: 0
                        player?.seekTo(newPos.coerceAtMost(duration))

                        seekHideHandler.removeCallbacks(hideSeekForwardRunnable)
                        binding.layoutSeekForward.visibility = View.VISIBLE
                        binding.tvSeekForwardSeconds.text = "$seekForwardAmount seconds"
                        seekHideHandler.postDelayed(hideSeekForwardRunnable, 800)
                    }
                    return true
                }

                override fun onDown(e: android.view.MotionEvent): Boolean = true
            }
        )

        // 3. GỘP CHUNG BẮT SỰ KIỆN CHẠM VÀO ĐÂY (Giải quyết xung đột)
        binding.playerView.setOnTouchListener { _, event ->
            // Đẩy sự kiện cho GestureDetector xử lý Double Tap
            gestureDetector.onTouchEvent(event)

            // Tự xử lý sự kiện nhả tay ra (ACTION_UP/CANCEL) để tắt tua 2.0x
            when (event.action) {
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    if (layoutSpeedIndicator.visibility == View.VISIBLE) {
                        player?.setPlaybackSpeed(1.0f)
                        layoutSpeedIndicator.visibility = View.GONE
                    }
                }
            }
            false // Trả về false để không chặn click mặc định
        }
    }

    override fun onResume() {
        super.onResume()
        // Quay lại app → stop service
        stopService(Intent(this, MediaPlaybackService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MediaPlaybackService::class.java))
        player?.release()
        player = null
    }

}