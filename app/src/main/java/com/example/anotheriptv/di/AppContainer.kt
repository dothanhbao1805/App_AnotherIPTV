package com.example.anotheriptv.di

import android.content.Context
import androidx.room.Room
import com.example.anotheriptv.data.local.database.AppDatabase
import com.example.anotheriptv.data.local.datastore.SettingsDataStore
import com.example.anotheriptv.data.mapper.ChannelMapper
import com.example.anotheriptv.data.mapper.HistoryMapper
import com.example.anotheriptv.data.mapper.PlaylistMapper
import com.example.anotheriptv.data.remote.parser.M3UParser
import com.example.anotheriptv.data.repository.ChannelRepositoryImpl
import com.example.anotheriptv.data.repository.PlaylistRepositoryImpl
import com.example.anotheriptv.data.repository.WatchHistoryRepositoryImpl
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.repository.PlaylistRepository
import com.example.anotheriptv.domain.repository.SettingsRepository
import com.example.anotheriptv.domain.repository.WatchHistoryRepository
import com.example.anotheriptv.domain.usecase.channel.GetChannelsUseCase
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.playlist.AddPlaylistUseCase
import com.example.anotheriptv.domain.usecase.playlist.DeletePlaylistUseCase
import com.example.anotheriptv.domain.usecase.playlist.GetPlaylistsUseCase
import okhttp3.OkHttpClient
import kotlin.jvm.java


class AppContainer(context: Context) {

    // ── Database ──
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "anotheriptv.db"
    ).build()

    // ── DAO ──
    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()
    private val historyDao = database.watchHistoryDao()

    // ── DataStore ──
    private val settingsDataStore = SettingsDataStore(context)

    // ── Mapper ──
    private val playlistMapper = PlaylistMapper()
    private val channelMapper = ChannelMapper()
    private val historyMapper = HistoryMapper()

    // ── Parser + Network ──
    private val okHttpClient = OkHttpClient()
    private val m3uParser = M3UParser()

    // ── Repository ──
    private val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl(
        playlistDao = playlistDao,
        channelDao = channelDao,
        playlistMapper = playlistMapper,
        channelMapper = channelMapper,
        m3uParser = m3uParser,
        okHttpClient = okHttpClient
    )

    private val channelRepository: ChannelRepository = ChannelRepositoryImpl(
        channelDao = channelDao,
        channelMapper = channelMapper
    )

    private val historyRepository: WatchHistoryRepository = WatchHistoryRepositoryImpl(
        historyDao = historyDao,
        historyMapper = historyMapper
    )


    // ── UseCase: Playlist ──
    val getPlaylistsUseCase = GetPlaylistsUseCase(playlistRepository)
    val addPlaylistUseCase = AddPlaylistUseCase(playlistRepository)
    val deletePlaylistUseCase = DeletePlaylistUseCase(playlistRepository)

    // ── UseCase: Channel ──
    val getChannelsUseCase = GetChannelsUseCase(channelRepository)

    // ── UseCase: History ──
    val addWatchHistoryUseCase = AddWatchHistoryUseCase(historyRepository)

}