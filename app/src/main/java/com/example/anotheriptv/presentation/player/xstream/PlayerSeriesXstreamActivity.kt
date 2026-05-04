package com.example.anotheriptv.presentation.player.xstream

import android.content.Context
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
import com.example.anotheriptv.databinding.ActivityPlayerSeriesXstreamBinding
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.model.Season
import com.example.anotheriptv.presentation.player.xstream.Adapter.ChannelListAdapter
import com.example.anotheriptv.presentation.player.xstream.Adapter.SeriesAdapter
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
class PlayerSeriesXstreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerSeriesXstreamBinding
    private var player: ExoPlayer? = null
    private var isInfoVisible = false

    private var channelName: String = ""
    private var streamUrl: String = ""
    private var playlistId: Long = -1L
    private var seriesId: Long = -1L
    private var currentSeasonNumber: Int = 1

    private var isListVisible = false
    private var isShowingEpisodes = false
    private var isSettingVisible = false

    // Data
    private var allEpisodes: List<Channel> = emptyList()
    private var seasons: List<Season> = emptyList()
    private var currentSeason: Season? = null

    private var videoTrackAuto = true
    private var audioTrackAuto = true
    private var subtitleTrackAuto = true

    private var playingSeasonNumber: Int = 1

    private val hideControlsRunnable = Runnable { hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerSeriesXstreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelName       = intent.getStringExtra("channelName") ?: "Episode"
        streamUrl         = intent.getStringExtra("streamUrl")   ?: return
        playlistId        = intent.getLongExtra("playlistId", -1L)
        seriesId          = intent.getLongExtra("seriesId", -1L)
        currentSeasonNumber = intent.getIntExtra("seasonNumber", 1)

        binding.tvChannelName.text = channelName

        playingSeasonNumber = intent.getIntExtra("seasonNumber", 1)
        currentSeasonNumber = playingSeasonNumber

        setupPlayer(streamUrl)
        setupControls()
        setupSettingPanel()
        setupSpeedGesture()
        setupListPanel()
        loadSeriesData()
    }

    // ── Load data ─────────────────────────────────────────────────────────────

    private fun loadSeriesData() {
        if (seriesId == -1L || playlistId == -1L) return

        lifecycleScope.launch(Dispatchers.IO) {
            val container = (application as MyApp).container

            // Collect một lần từ Flow
            container.channelRepository
                .getEpisodesBySeriesId(playlistId, seriesId)
                .collect { episodes ->
                    allEpisodes = episodes

                    // Build seasons từ episodes
                    seasons = episodes
                        .filter { it.seasonNumber != null }
                        .groupBy { it.seasonNumber!! }
                        .map { (seasonNum, eps) ->
                            Season(
                                seasonNumber = seasonNum,
                                name         = "Season $seasonNum",
                                episodeCount = eps.size
                            )
                        }
                        .sortedBy { it.seasonNumber }

                    // Set current season
                    currentSeason = seasons.find { it.seasonNumber == currentSeasonNumber }
                        ?: seasons.firstOrNull()
                    currentSeasonNumber = currentSeason?.seasonNumber ?: currentSeasonNumber

                    // Nếu list panel đang mở thì refresh
                    withContext(Dispatchers.Main) {
                        if (isListVisible) {
                            if (isShowingEpisodes) showEpisodesOfSeason(currentSeasonNumber)
                            else showSeasons()
                        }
                    }
                }
        }
    }

    // ── OkHttp ────────────────────────────────────────────────────────────────

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

    // ── Player ────────────────────────────────────────────────────────────────

    private fun setupPlayer(url: String) {
        val dataSourceFactory = DefaultDataSource.Factory(
            this, OkHttpDataSource.Factory(buildOkHttpClient())
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
                                binding.progressBuffering.visibility = View.VISIBLE
                                binding.btnPlay.visibility           = View.GONE
                                binding.btnPause.visibility          = View.GONE
                            }
                            Player.STATE_READY -> {
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

    // ── Controls ──────────────────────────────────────────────────────────────

    private fun setupControls() {
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

        binding.btnInfo.setOnClickListener { toggleInfoPanel() }

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
            if (isInfoVisible) { hideInfoPanel(); return@setOnClickListener }
            if (binding.layoutTopControls.visibility == View.VISIBLE) hideControls()
            else { showControls(); scheduleHideControls() }
        }
    }

    // ── List Panel ────────────────────────────────────────────────────────────

    private fun setupListPanel() {
        binding.layoutListPanel.btnCloseList.setOnClickListener { hideListPanel() }
        binding.layoutListPanel.btnListBack.setOnClickListener  { showSeasons() }
        binding.btnList.setOnClickListener { toggleListPanel() }
    }

    private fun toggleListPanel() {
        if (isListVisible) hideListPanel() else showListPanel()
    }

    private fun showListPanel() {
        isListVisible = true
        binding.layoutListPanel.root.visibility = View.VISIBLE

        // Luôn mở thẳng vào episode list của season hiện tại
        if (seasons.isEmpty()) {
            // Data chưa load xong → đợi loadSeriesData() callback
            binding.layoutListPanel.tvListTitle.text = "Loading..."
        } else {
            showEpisodesOfSeason(currentSeasonNumber)
        }
    }

    private fun hideListPanel() {
        isListVisible = false
        binding.layoutListPanel.root.visibility = View.GONE
    }

    private fun showSeasons() {
        isShowingEpisodes = false
        binding.layoutListPanel.tvListTitle.text = "Seasons"
        binding.layoutListPanel.btnListBack.visibility = View.GONE

        val playingSeasonName = seasons.find { it.seasonNumber == playingSeasonNumber }?.name
            ?: "Season $playingSeasonNumber"

        val focusSeasonName = seasons.find { it.seasonNumber == currentSeasonNumber }?.name
            ?: playingSeasonName

        val seasonItems = seasons.map {
            SeriesAdapter.SeriesItem(name = it.name, episodeCount = it.episodeCount)
        }

        binding.layoutListPanel.rvList.layoutManager = LinearLayoutManager(this)
        binding.layoutListPanel.rvList.adapter = SeriesAdapter(
            items         = seasonItems,
            currentSeries = playingSeasonName
        ) { selected ->
            val seasonNum = seasons.find { it.name == selected.name }?.seasonNumber
                ?: playingSeasonNumber
            currentSeasonNumber = seasonNum
            currentSeason       = seasons.find { it.seasonNumber == seasonNum }
            showEpisodesOfSeason(seasonNum)
        }

        val focusIndex = seasonItems.indexOfFirst { it.name == focusSeasonName }
        if (focusIndex >= 0) {
            binding.layoutListPanel.rvList.scrollToPosition(focusIndex)
        }
    }

    private fun showEpisodesOfSeason(seasonNumber: Int) {
        isShowingEpisodes = true
        val seasonName = seasons.find { it.seasonNumber == seasonNumber }?.name
            ?: "Season $seasonNumber"
        binding.layoutListPanel.tvListTitle.text = seasonName
        binding.layoutListPanel.btnListBack.visibility = View.VISIBLE

        val episodes = allEpisodes
            .filter { it.seasonNumber == seasonNumber }
            .sortedBy { it.episodeNumber }

        val existingAdapter = binding.layoutListPanel.rvList.adapter
        if (existingAdapter is ChannelListAdapter &&
            binding.layoutListPanel.tvListTitle.text == seasonName) {
            existingAdapter.updateCurrentUrl(streamUrl)
        } else {
            binding.layoutListPanel.rvList.layoutManager = LinearLayoutManager(this)
            binding.layoutListPanel.rvList.adapter = ChannelListAdapter(
                items      = episodes,
                currentUrl = streamUrl
            ) { selected ->
                switchEpisode(selected.name, selected.url ?: "", seasonNumber)
            }
        }

        val currentIndex = episodes.indexOfFirst { it.url == streamUrl }
        if (currentIndex >= 0) {
            binding.layoutListPanel.rvList.scrollToPosition(currentIndex)
        }
    }

    private fun switchEpisode(name: String, url: String, seasonNumber: Int) {
        channelName         = name
        streamUrl           = url
        currentSeasonNumber = seasonNumber
        currentSeason       = seasons.find { it.seasonNumber == seasonNumber }
        binding.tvChannelName.text = channelName

        playingSeasonNumber = seasonNumber
        currentSeasonNumber = seasonNumber

        player?.let { exo ->
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.play()
        }

        binding.progressBuffering.visibility = View.VISIBLE
        binding.btnPlay.visibility  = View.GONE
        binding.btnPause.visibility = View.GONE

        // Refresh episode list để update highlight
        showEpisodesOfSeason(seasonNumber)
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

    // ── Info Panel ────────────────────────────────────────────────────────────

    private fun toggleInfoPanel() {
        if (isInfoVisible) hideInfoPanel() else showInfoPanel()
    }

    private fun showInfoPanel() {
        isInfoVisible = true
        binding.layoutInfoPanel.tvInfoName.text        = channelName
        binding.layoutInfoPanel.tvInfoContentType.text = "Series"
        binding.layoutInfoPanel.tvInfoEpgId.text       = intent.getStringExtra("epgId") ?: "—"
        binding.layoutInfoPanel.tvInfoStreamId.text    = intent.getStringExtra("streamId") ?: "—"
        binding.layoutInfoPanel.tvInfoUrl.text         = streamUrl
        binding.layoutInfoPanel.root.visibility        = View.VISIBLE

        binding.layoutInfoPanel.btnCopyStreamId.setOnClickListener {
            copyToClipboard("Stream ID", binding.layoutInfoPanel.tvInfoStreamId.text.toString())
        }
        binding.layoutInfoPanel.btnCopyUrl.setOnClickListener {
            copyToClipboard("URL", streamUrl)
        }
        binding.layoutInfoPanel.btnCloseInfo.setOnClickListener { hideInfoPanel() }
    }

    private fun hideInfoPanel() {
        isInfoVisible = false
        binding.layoutInfoPanel.root.visibility = View.GONE
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
        Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
    }

    // ── Setting Panel ─────────────────────────────────────────────────────────

    private fun setupSettingPanel() {
        binding.layoutSettingPanel.btnCloseSetting.setOnClickListener { hideSettingPanel() }

        binding.layoutSettingPanel.btnVideoAuto.setOnClickListener {
            videoTrackAuto = true; updateVideoTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, false).build()
        }
        binding.layoutSettingPanel.btnVideoDisabled.setOnClickListener {
            videoTrackAuto = false; updateVideoTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, true).build()
        }
        binding.layoutSettingPanel.btnAudioAuto.setOnClickListener {
            audioTrackAuto = true; updateAudioTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, false).build()
        }
        binding.layoutSettingPanel.btnAudioDisabled.setOnClickListener {
            audioTrackAuto = false; updateAudioTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true).build()
        }
        binding.layoutSettingPanel.btnSubtitleAuto.setOnClickListener {
            subtitleTrackAuto = true; updateSubtitleTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false).build()
        }
        binding.layoutSettingPanel.btnSubtitleDisabled.setOnClickListener {
            subtitleTrackAuto = false; updateSubtitleTrackUI()
            player?.trackSelectionParameters = player!!.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true).build()
        }
    }

    private fun updateVideoTrackUI() {
        binding.layoutSettingPanel.btnVideoAuto.setBackgroundResource(
            if (videoTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option)
        binding.layoutSettingPanel.ivVideoAutoCheck.visibility =
            if (videoTrackAuto) View.VISIBLE else View.GONE
        binding.layoutSettingPanel.btnVideoDisabled.setBackgroundResource(
            if (!videoTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option)
        binding.layoutSettingPanel.ivVideoDisabledCheck.visibility =
            if (!videoTrackAuto) View.VISIBLE else View.GONE
    }

    private fun updateAudioTrackUI() {
        binding.layoutSettingPanel.btnAudioAuto.setBackgroundResource(
            if (audioTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option)
        binding.layoutSettingPanel.ivAudioAutoCheck.visibility =
            if (audioTrackAuto) View.VISIBLE else View.GONE
        binding.layoutSettingPanel.btnAudioDisabled.setBackgroundResource(
            if (!audioTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option)
        binding.layoutSettingPanel.ivAudioDisabledCheck.visibility =
            if (!audioTrackAuto) View.VISIBLE else View.GONE
    }

    private fun updateSubtitleTrackUI() {
        binding.layoutSettingPanel.btnSubtitleAuto.setBackgroundResource(
            if (subtitleTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option)
        binding.layoutSettingPanel.ivSubtitleAutoCheck.visibility =
            if (subtitleTrackAuto) View.VISIBLE else View.GONE
        binding.layoutSettingPanel.btnSubtitleDisabled.setBackgroundResource(
            if (!subtitleTrackAuto) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_option)
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

    private fun setupSpeedGesture() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val layoutSpeedIndicator = binding.layoutSpeedIndicator

        binding.playerView.setOnLongClickListener {
            if (prefs.getBoolean("speed_up_long_press", true)) {
                // Tăng tốc độ lên 2.0x
                player?.setPlaybackSpeed(2.0f)
                layoutSpeedIndicator.visibility = View.VISIBLE
                // Ẩn controls khi đang speed up
                hideControls()
            }
            true
        }

        binding.playerView.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    if (layoutSpeedIndicator.visibility == View.VISIBLE) {
                        // Thả tay → về tốc độ bình thường
                        player?.setPlaybackSpeed(1.0f)
                        layoutSpeedIndicator.visibility = View.GONE
                    }
                }
            }
            // Trả về false để không block các gesture khác (click, seek...)
            false
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

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