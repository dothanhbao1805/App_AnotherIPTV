package com.example.anotheriptv.presentation.xstream.series.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.data.local.dao.ChannelDao
import com.example.anotheriptv.data.remote.parser.XstreamParser
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.model.Season
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SeriesXstreamViewModel(
    private val channelRepository: ChannelRepository,
    private val categoryDao: CategoryDao,
    private val addWatchHistoryUseCase: AddWatchHistoryUseCase,
    private val channelDao: ChannelDao,
    private val xstreamParser: XstreamParser
) : ViewModel() {

    private val _categoriesWithChannels = MutableStateFlow<List<CategoryWithChannels>>(emptyList())
    val categoriesWithChannels: StateFlow<List<CategoryWithChannels>> = _categoriesWithChannels.asStateFlow()

    private val _allSeries = MutableStateFlow<List<Channel>>(emptyList())
    val allSeries: StateFlow<List<Channel>> = _allSeries.asStateFlow()

    private val _seasons = MutableStateFlow<List<Season>>(emptyList())
    val seasons: StateFlow<List<Season>> = _seasons.asStateFlow()

    private val _episodes = MutableStateFlow<List<Channel>>(emptyList())
    val episodes: StateFlow<List<Channel>> = _episodes.asStateFlow()

    private val _isLoadingSeasons = MutableStateFlow(false)
    val isLoadingSeasons: StateFlow<Boolean> = _isLoadingSeasons.asStateFlow()

    private var seasonsJob: kotlinx.coroutines.Job? = null
    private var episodesJob: kotlinx.coroutines.Job? = null

    private var currentSeriesId: Long? = null
    private var fetchJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────

    fun loadLiveChannels(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = categoryDao.getVisibleCategoriesByPlaylistAndType(playlistId, "SERIES")
            val result = categories.map { cat ->
                val channels = channelRepository.getChannelsByCategoryLimit10(
                    playlistId  = playlistId,
                    contentType = "SERIES",
                    categoryId  = cat.categoryId
                )
                CategoryWithChannels(
                    categoryId   = cat.categoryId,
                    categoryName = cat.name,
                    channels     = channels
                )
            }.filter { it.channels.isNotEmpty() }
            _categoriesWithChannels.value = result
        }
    }

    fun loadAllSeries(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelRepository.getChannelsByContentType(playlistId, "SERIES").collect { all ->
                // Chỉ lấy series đại diện: không có seasonNumber (url rỗng, không phải episode)
                _allSeries.value = all.filter {
                    it.seasonNumber == null && it.seriesId != null
                }
            }
        }
    }

    fun fetchAndLoadSeasons(
        playlistId: Long,
        seriesId: Long,
        baseUrl: String,
        username: String,
        password: String,
        seriesName: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _seasons.value = emptyList()
            _episodes.value = emptyList()

            _isLoadingSeasons.value = true
            try {
                val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_series_info&series_id=$seriesId"
                val json = downloadText(url)

                if (json != null) {
                    val episodes = xstreamParser.parseEpisodes(
                        json       = json,
                        playlistId = playlistId,
                        seriesName = seriesName,
                        baseUrl    = baseUrl,
                        username   = username,
                        password   = password,
                        seriesId   = seriesId
                    )

                    if (episodes.isNotEmpty()) {
                        channelDao.insertAll(episodes)
                    }
                }
                loadSeasons(playlistId, seriesId)
            } catch (e: Exception) {
                android.util.Log.e("SeriesDebug", "Error: ${e.message}")
                loadSeasons(playlistId, seriesId)
            } finally {
                _isLoadingSeasons.value = false
            }
        }
    }

    fun loadSeasons(playlistId: Long, seriesId: Long) {
        seasonsJob?.cancel()
        seasonsJob = viewModelScope.launch(Dispatchers.IO) {
            channelRepository.getEpisodesBySeriesId(playlistId, seriesId).collect { episodeList ->

                val processedSeasons = episodeList
                    .filter { it.seasonNumber != null }
                    .groupBy { it.seasonNumber!! }
                    .map { entry ->
                        val seasonNum = entry.key
                        val episodesInSeason = entry.value

                        // Lấy ngày phát hành của tập đầu tiên trong mùa làm ngày đại diện cho mùa
                        val firstEpisodeDate = episodesInSeason
                            .sortedBy { it.episodeNumber }
                            .firstOrNull()?.releaseDate

                        Season(
                            seasonNumber = seasonNum,
                            name = "Season $seasonNum",
                            episodeCount = episodesInSeason.size,
                            releaseDate = firstEpisodeDate
                        )
                    }
                    .sortedBy { it.seasonNumber }

                _seasons.value = processedSeasons
            }
        }
    }

    fun loadEpisodes(playlistId: Long, seriesId: Long, seasonNumber: Int) {
        // Hủy job cũ trước khi lắng nghe danh sách tập phim mới
        episodesJob?.cancel()

        episodesJob = viewModelScope.launch(Dispatchers.IO) {
            channelRepository.getEpisodesBySeriesId(playlistId, seriesId).collect { allEpisodes ->
                _episodes.value = allEpisodes
                    .filter { it.seasonNumber == seasonNumber }
                    .sortedBy { it.episodeNumber }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        seasonsJob?.cancel()
        episodesJob?.cancel()
    }


    fun addToHistory(
        channelId: Long,
        playlistId: Long,
        channelName: String,
        channelLogo: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val historyItem = WatchHistory(
                id              = 0,
                channelId       = channelId,
                playlistId      = playlistId,
                channelName     = channelName,
                channelLogo     = channelLogo,
                streamUrl       = "",
                watchedAt       = System.currentTimeMillis()
            )
            addWatchHistoryUseCase(historyItem)
        }
    }

    fun clearSeriesData() {
        _seasons.value = emptyList()
        _episodes.value = emptyList()
    }

    fun checkAndClearOldData(newSeriesId: Long) {
        if (currentSeriesId != newSeriesId) {
            _seasons.value = emptyList()
            _episodes.value = emptyList()
            currentSeriesId = newSeriesId
        }
    }

    // ── Network helper ────────────────────────────────────────────────────────

    private suspend fun downloadText(url: String): String? = try {
        val client = io.ktor.client.HttpClient(io.ktor.client.engine.android.Android) {
            engine {
                connectTimeout = 30_000
                socketTimeout  = 60_000
            }
        }
        val response = client.get(url) {
            headers {
                append("User-Agent", "okhttp/3.12.1")
                append("Accept", "*/*")
            }
        }
        client.close()
        response.bodyAsText()
    } catch (e: Exception) {
        android.util.Log.e("SeriesVM", "downloadText: ${e.message}")
        null
    }


}