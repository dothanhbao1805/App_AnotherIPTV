package com.example.anotheriptv.data.repository

import com.example.anotheriptv.data.local.dao.ChannelDao
import com.example.anotheriptv.data.local.dao.PlaylistDao
import com.example.anotheriptv.data.mapper.ChannelMapper
import com.example.anotheriptv.data.mapper.PlaylistMapper
import com.example.anotheriptv.data.remote.parser.M3UParser
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.OkHttpClient
import java.io.File


class PlaylistRepositoryImpl(
    private val context: android.content.Context,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val playlistMapper: PlaylistMapper,
    private val channelMapper: ChannelMapper,
    private val m3uParser: M3UParser,
    private val okHttpClient: OkHttpClient
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

            val content = fetchM3UContent(playlist)
                ?: throw Exception("Không lấy được dữ liệu từ URL")

            val entity = playlistMapper.toEntity(playlist)
            val playlistId = playlistDao.insert(entity)

            val channels = m3uParser.parse(content, playlistId)
            channelDao.insertAll(channels)

            playlistId
        }
    }

    override suspend fun deletePlaylist(id: Long) {
        playlistDao.deleteById(id)
        channelDao.deleteByPlaylistId(id)
    }

    private suspend fun fetchAndSaveChannels(playlistId: Long, playlist: Playlist) {
        val content = when (playlist.type) {
            "M3U"     -> fetchM3UContent(playlist)
            "XSTREAM" -> fetchXStreamContent(playlist)
            else      -> return
        }
        if (content.isNullOrEmpty()) return
        val channelEntities = m3uParser.parse(content, playlistId)
        channelDao.insertAll(channelEntities)
    }

    private suspend fun fetchM3UContent(playlist: Playlist): String? {
        return when (playlist.sourceType) {
            "URL"  -> downloadText(playlist.m3uUrl ?: return null)
            "FILE" -> readFile(playlist.filePath ?: return null)
            else   -> null
        }
    }

    private suspend fun fetchXStreamContent(playlist: Playlist): String? {
        val url = "${playlist.url}/get.php" +
                "?username=${playlist.userName}" +
                "&password=${playlist.password}" +
                "&type=m3u_plus&output=ts"
        return downloadText(url)
    }

    private suspend fun downloadText(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun readFile(filePath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(filePath)
                // Nếu là content:// URI thì dùng ContentResolver
                if (uri.scheme == "content") {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    }
                } else {
                    // Nếu là file path thông thường
                    java.io.File(filePath).readText()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

}