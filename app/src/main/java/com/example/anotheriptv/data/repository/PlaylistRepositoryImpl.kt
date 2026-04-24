package com.example.anotheriptv.data.repository

import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.data.local.dao.ChannelDao
import com.example.anotheriptv.data.local.dao.PlaylistDao
import com.example.anotheriptv.data.local.entity.CategoryEntity
import com.example.anotheriptv.data.local.entity.ChannelEntity
import com.example.anotheriptv.data.mapper.ChannelMapper
import com.example.anotheriptv.data.mapper.PlaylistMapper
import com.example.anotheriptv.data.remote.parser.M3UParser
import com.example.anotheriptv.data.remote.parser.XstreamParser
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText

class PlaylistRepositoryImpl(
    private val context: android.content.Context,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val playlistMapper: PlaylistMapper,
    private val channelMapper: ChannelMapper,
    private val m3uParser: M3UParser,
    private val xstreamParser: XstreamParser,
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAll()
            .map { entities -> entities.map { playlistMapper.toDomain(it) } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getPlaylistById(id: Long): Playlist? {
        return playlistDao.getById(id)?.let { playlistMapper.toDomain(it) }
    }

    override suspend fun addPlaylist(playlist: Playlist): Long {
        return withContext(Dispatchers.IO) {

            val channels: List<ChannelEntity> = when (playlist.type) {
                "M3U" -> {
                    val content = fetchM3UContent(playlist)
                        ?: throw Exception("Không lấy được dữ liệu M3U")
                    val parsed = m3uParser.parse(content, 0L) // playlistId tạm = 0
                    if (parsed.isEmpty()) throw Exception("File M3U không có kênh nào")
                    parsed
                }
                "XSTREAM" -> {
                    val baseUrl  = playlist.url      ?: throw Exception("Thiếu URL Xtream")
                    val username = playlist.userName ?: throw Exception("Thiếu username")
                    val password = playlist.password ?: throw Exception("Thiếu password")
                    // Chỉ verify kết nối, chưa fetch hết
                    val infoUrl = buildXstreamUrl(baseUrl, username, password, "get_account_info")
                    downloadText(infoUrl) ?: throw Exception("Không kết nối được tới server Xtream")
                    emptyList()
                }
                else -> throw Exception("Loại playlist không hợp lệ: ${playlist.type}")
            }

            val entity     = playlistMapper.toEntity(playlist)
            val playlistId = playlistDao.insert(entity)

            when (playlist.type) {
                "M3U" -> {
                    val fixed = channels.map { it.copy(playlistId = playlistId) }
                    channelDao.insertAll(fixed)
                }
                "XSTREAM" -> {
                    handleXstream(playlistId, playlist)
                }
            }

            playlistId
        }
    }

    override suspend fun addPlaylistXstream(
        playlist: Playlist,
        onProgress: (progress: Int, status: String) -> Unit
    ): Long {
        return withContext(Dispatchers.IO) {

            val baseUrl  = playlist.url      ?: throw Exception("Thiếu URL Xtream")
            val username = playlist.userName ?: throw Exception("Thiếu username")
            val password = playlist.password ?: throw Exception("Thiếu password")

            // ── Bước 1: Verify kết nối TRƯỚC khi insert DB ──
            onProgress(10, "Connecting to server...")
            val infoUrl = buildXstreamUrl(baseUrl, username, password, "get_account_info")
            downloadText(infoUrl) ?: throw Exception("Không kết nối được tới server Xtream")

            // ── Bước 2: Fetch categories ──
            onProgress(20, "Preparing categories...")
            val liveCatMap: Map<String, String>
            val movieCatMap: Map<String, String>
            val seriesCatMap: Map<String, String>

            coroutineScope {
                val liveC   = async { fetchCategoryMap(baseUrl, username, password, "get_live_categories") }
                val movieC  = async { fetchCategoryMap(baseUrl, username, password, "get_vod_categories") }
                val seriesC = async { fetchCategoryMap(baseUrl, username, password, "get_series_categories") }
                liveCatMap   = liveC.await()
                movieCatMap  = movieC.await()
                seriesCatMap = seriesC.await()
            }

            // ── Bước 3: Fetch channels (chưa insert DB) ──
            onProgress(30, "Loading live channels...")
            val live = fetchXstreamLive(baseUrl, username, password, 0L, liveCatMap)

            onProgress(55, "Loading movies...")
            val movies = fetchXstreamMovies(baseUrl, username, password, 0L, movieCatMap)

            onProgress(75, "Loading series...")
            val series = fetchXstreamSeries(baseUrl, username, password, 0L, seriesCatMap)

            val allChannels = live + movies + series
            if (allChannels.isEmpty()) throw Exception("Server không trả về kênh nào")

            // ── Bước 4: Chỉ insert playlist sau khi fetch thành công ──
            onProgress(85, "Saving playlist...")
            val entity     = playlistMapper.toEntity(playlist)
            val playlistId = playlistDao.insert(entity)

            // ── Bước 5: Insert categories + channels với đúng playlistId ──
            onProgress(90, "Saving to database...")
            val categoryEntities = mutableListOf<CategoryEntity>()
            liveCatMap.forEach   { (id, name) -> categoryEntities.add(CategoryEntity(playlistId, id, "LIVE",   name)) }
            movieCatMap.forEach  { (id, name) -> categoryEntities.add(CategoryEntity(playlistId, id, "MOVIE",  name)) }
            seriesCatMap.forEach { (id, name) -> categoryEntities.add(CategoryEntity(playlistId, id, "SERIES", name)) }
            categoryDao.insertAll(categoryEntities)

            val fixedChannels = allChannels.map { it.copy(playlistId = playlistId) }
            channelDao.insertAll(fixedChannels)

            onProgress(100, "Complete!")
            playlistId
        }
    }

    private suspend fun handleXstreamWithProgress(
        playlistId: Long,
        playlist: Playlist,
        onProgress: (Int, String) -> Unit
    ) {
        val baseUrl  = playlist.url      ?: throw Exception("Thiếu URL Xtream")
        val username = playlist.userName ?: throw Exception("Thiếu username")
        val password = playlist.password ?: throw Exception("Thiếu password")

        // Verify server
        onProgress(10, "Connecting to server...")
        val infoUrl = buildXstreamUrl(baseUrl, username, password, "get_account_info")
        downloadText(infoUrl) ?: throw Exception("Không kết nối được tới server Xtream")

        // Fetch categories song song
        onProgress(20, "Preparing categories...")
        val liveCatMap: Map<String, String>
        val movieCatMap: Map<String, String>
        val seriesCatMap: Map<String, String>

        coroutineScope {
            val liveC   = async { fetchCategoryMap(baseUrl, username, password, "get_live_categories") }
            val movieC  = async { fetchCategoryMap(baseUrl, username, password, "get_vod_categories") }
            val seriesC = async { fetchCategoryMap(baseUrl, username, password, "get_series_categories") }
            liveCatMap   = liveC.await()
            movieCatMap  = movieC.await()
            seriesCatMap = seriesC.await()
        }

        // Lưu categories vào DB
        val categoryEntities = mutableListOf<CategoryEntity>()
        liveCatMap.forEach   { (id, name) -> categoryEntities.add(CategoryEntity(playlistId, id, "LIVE",   name)) }
        movieCatMap.forEach  { (id, name) -> categoryEntities.add(CategoryEntity(playlistId, id, "MOVIE",  name)) }
        seriesCatMap.forEach { (id, name) -> categoryEntities.add(CategoryEntity(playlistId, id, "SERIES", name)) }
        categoryDao.insertAll(categoryEntities)

        val allChannels = mutableListOf<ChannelEntity>()

        // Fetch Live
        onProgress(30, "Loading live channels...")
        val live = fetchXstreamLive(baseUrl, username, password, playlistId, liveCatMap)
        allChannels.addAll(live)

        // Fetch Movies
        onProgress(55, "Loading movies...")
        val movies = fetchXstreamMovies(baseUrl, username, password, playlistId, movieCatMap)
        allChannels.addAll(movies)

        // Fetch Series
        onProgress(75, "Loading series...")
        val series = fetchXstreamSeries(baseUrl, username, password, playlistId, seriesCatMap)
        allChannels.addAll(series)

        if (allChannels.isEmpty()) throw Exception("Server không trả về kênh nào")

        onProgress(90, "Saving to database...")
        channelDao.insertAll(allChannels)

        onProgress(100, "Complete!")
    }

    // Fetch category map: categoryId -> categoryName
    private suspend fun fetchCategoryMap(
        baseUrl: String, username: String, password: String, action: String
    ): Map<String, String> = try {
        val url  = buildXstreamUrl(baseUrl, username, password, action)
        val json = downloadText(url) ?: return emptyMap()
        val array = org.json.JSONArray(json)
        buildMap {
            for (i in 0 until array.length()) {
                val obj  = array.optJSONObject(i) ?: continue
                val id   = obj.optString("category_id", "")
                val name = obj.optString("category_name", "Uncategorized")
                if (id.isNotEmpty()) put(id, name)
            }
        }
    } catch (e: Exception) {
        emptyMap()
    }
    private suspend fun fetchXstreamLive(
        baseUrl: String, username: String, password: String,
        playlistId: Long,
        categoryMap: Map<String, String> = emptyMap()
    ): List<ChannelEntity> = try {
        val url  = buildXstreamUrl(baseUrl, username, password, "get_live_streams")
        val json = downloadText(url) ?: return emptyList()
        xstreamParser.parseLive(json, playlistId, baseUrl, username, password, categoryMap)
    } catch (e: Exception) { emptyList() }

    private suspend fun fetchXstreamMovies(
        baseUrl: String, username: String, password: String,
        playlistId: Long,
        categoryMap: Map<String, String> = emptyMap()
    ): List<ChannelEntity> = try {
        val url  = buildXstreamUrl(baseUrl, username, password, "get_vod_streams")
        val json = downloadText(url) ?: return emptyList()
        xstreamParser.parseMovies(json, playlistId, baseUrl, username, password, categoryMap)
    } catch (e: Exception) { emptyList() }

    private suspend fun fetchXstreamSeries(
        baseUrl: String, username: String, password: String,
        playlistId: Long,
        categoryMap: Map<String, String> = emptyMap()
    ): List<ChannelEntity> = try {
        val url  = buildXstreamUrl(baseUrl, username, password, "get_series")
        val json = downloadText(url) ?: return emptyList()
        xstreamParser.parseSeries(json, playlistId, baseUrl, username, password, categoryMap)
    } catch (e: Exception) { emptyList() }


    override suspend fun deletePlaylist(id: Long) {
        playlistDao.deleteById(id)
        channelDao.deleteByPlaylistId(id)
        categoryDao.deleteByPlaylistId(id)
    }

    // ─── M3U ────────────────────────────────────────────────────────────────────

    private suspend fun handleM3U(playlistId: Long, playlist: Playlist) {
        val content = fetchM3UContent(playlist)
            ?: throw Exception("Không lấy được dữ liệu M3U")

        val channels = m3uParser.parse(content, playlistId)
        if (channels.isEmpty()) throw Exception("File M3U không có kênh nào")

        channels.take(5).forEach { channel ->
            android.util.Log.d("M3UDebug", "name=${channel.name}, logo=${channel.logo}, url=${channel.url}")
        }

        channelDao.insertAll(channels)
    }

    private suspend fun fetchM3UContent(playlist: Playlist): String? {
        return when (playlist.sourceType) {
            "URL"  -> downloadText(playlist.m3uUrl ?: return null)
            "FILE" -> readFile(playlist.filePath ?: return null)
            else   -> null
        }
    }

    // ─── XSTREAM ────────────────────────────────────────────────────────────────

    private suspend fun handleXstream(playlistId: Long, playlist: Playlist) {
        val baseUrl  = playlist.url ?: throw Exception("Thiếu URL Xtream")
        val username = playlist.userName ?: throw Exception("Thiếu username")
        val password = playlist.password ?: throw Exception("Thiếu password")

        val infoUrl  = buildXstreamUrl(baseUrl, username, password, "get_account_info")
        downloadText(infoUrl) ?: throw Exception("Không kết nối được tới server Xtream")

        coroutineScope {
            val liveDeferred   = async { fetchXstreamLive(baseUrl, username, password, playlistId) }
            val movieDeferred  = async { fetchXstreamMovies(baseUrl, username, password, playlistId) }
            val seriesDeferred = async { fetchXstreamSeries(baseUrl, username, password, playlistId) }

            // khai báo type tường minh để Kotlin biết đây là List<ChannelEntity>
            val all: List<ChannelEntity> = liveDeferred.await() +
                    movieDeferred.await() +
                    seriesDeferred.await()

            if (all.isEmpty()) throw Exception("Server không trả về kênh nào")
            channelDao.insertAll(all)
        }
    }

    private suspend fun fetchXstreamLive(
        baseUrl: String, username: String, password: String, playlistId: Long
    ): List<ChannelEntity> = try {
        val url  = buildXstreamUrl(baseUrl, username, password, "get_live_streams")
        val json = downloadText(url) ?: return@fetchXstreamLive emptyList()
        xstreamParser.parseLive(json, playlistId, baseUrl, username, password)
    } catch (e: Exception) {
        emptyList()
    }

    private suspend fun fetchXstreamMovies(
        baseUrl: String, username: String, password: String, playlistId: Long
    ): List<ChannelEntity> = try {
        val url  = buildXstreamUrl(baseUrl, username, password, "get_vod_streams")
        val json = downloadText(url) ?: return@fetchXstreamMovies emptyList()
        xstreamParser.parseMovies(json, playlistId, baseUrl, username, password)
    } catch (e: Exception) {
        emptyList()
    }

    private suspend fun fetchXstreamSeries(
        baseUrl: String, username: String, password: String, playlistId: Long
    ): List<ChannelEntity> = try {
        val url  = buildXstreamUrl(baseUrl, username, password, "get_series")
        val json = downloadText(url) ?: return@fetchXstreamSeries emptyList()
        xstreamParser.parseSeries(json, playlistId, baseUrl, username, password)
    } catch (e: Exception) {
        emptyList()
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private fun buildXstreamUrl(
        baseUrl: String, username: String, password: String, action: String
    ): String {
        val url = "$baseUrl/player_api.php?username=${username.trim()}&password=$password&action=$action"
        android.util.Log.d("XstreamDebug", "Built URL: $url")
        return url
    }

    private suspend fun downloadText(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = io.ktor.client.HttpClient(io.ktor.client.engine.android.Android) {
                    engine {
                        connectTimeout = 30_000
                        socketTimeout = 30_000
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
                android.util.Log.e("XstreamDebug", "Ktor failed: ${e.message}")
                null
            }
        }
    }

    private suspend fun readFile(filePath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(filePath)
                if (uri.scheme == "content") {
                    context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                } else {
                    java.io.File(filePath).readText()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

}