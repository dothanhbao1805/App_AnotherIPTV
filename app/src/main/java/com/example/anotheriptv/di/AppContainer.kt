package com.example.anotheriptv.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.anotheriptv.data.local.database.AppDatabase
import java.util.concurrent.TimeUnit
import com.example.anotheriptv.data.local.datastore.SettingsDataStore
import com.example.anotheriptv.data.mapper.ChannelMapper
import com.example.anotheriptv.data.mapper.HistoryMapper
import com.example.anotheriptv.data.mapper.PlaylistMapper
import com.example.anotheriptv.data.remote.parser.M3UParser
import com.example.anotheriptv.data.remote.parser.XstreamParser
import com.example.anotheriptv.data.repository.ChannelRepositoryImpl
import com.example.anotheriptv.data.repository.PlaylistRepositoryImpl
import com.example.anotheriptv.data.repository.WatchHistoryRepositoryImpl
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.repository.PlaylistRepository
import com.example.anotheriptv.domain.repository.SettingsRepository
import com.example.anotheriptv.domain.repository.WatchHistoryRepository
import com.example.anotheriptv.domain.usecase.channel.GetChannelsUseCase
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.history.DeleteWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.history.GetWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.playlist.AddPlaylistUseCase
import com.example.anotheriptv.domain.usecase.playlist.AddXstreamUseCase
import com.example.anotheriptv.domain.usecase.playlist.DeletePlaylistUseCase
import com.example.anotheriptv.domain.usecase.playlist.GetPlaylistsUseCase
import okhttp3.OkHttpClient

class AppContainer(context: Context) {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tạo bảng watch_history mới với playlistId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS watch_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        channelId INTEGER NOT NULL,
                        playlistId INTEGER NOT NULL,
                        channelName TEXT NOT NULL,
                        channelLogo TEXT NOT NULL,
                        watchedAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("DROP TABLE IF EXISTS watch_history")
                db.execSQL("ALTER TABLE watch_history_new RENAME TO watch_history")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Thêm các cột mới vào bảng channels
                db.execSQL("ALTER TABLE channels ADD COLUMN contentType TEXT NOT NULL DEFAULT 'LIVE'")
                db.execSQL("ALTER TABLE channels ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE channels ADD COLUMN genre TEXT")
                db.execSQL("ALTER TABLE channels ADD COLUMN releaseDate TEXT")
                db.execSQL("ALTER TABLE channels ADD COLUMN cast TEXT")
                db.execSQL("ALTER TABLE channels ADD COLUMN trailerUrl TEXT")
                db.execSQL("ALTER TABLE channels ADD COLUMN rating REAL")
                db.execSQL("ALTER TABLE channels ADD COLUMN seriesId INTEGER")
                db.execSQL("ALTER TABLE channels ADD COLUMN seasonNumber INTEGER")
                db.execSQL("ALTER TABLE channels ADD COLUMN episodeNumber INTEGER")
                db.execSQL("ALTER TABLE channels ADD COLUMN episodeDuration INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Thêm column categoryId vào bảng channels
                db.execSQL("ALTER TABLE channels ADD COLUMN categoryId TEXT NOT NULL DEFAULT ''")

                // Tạo bảng categories mới
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                playlistId  INTEGER NOT NULL,
                categoryId  TEXT    NOT NULL,
                contentType TEXT    NOT NULL,
                name        TEXT    NOT NULL,
                PRIMARY KEY (playlistId, categoryId, contentType)
            )
        """)
            }
        }

    }

    // ── Database ──
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "anotheriptv.db"
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3,MIGRATION_3_4)
        .build()

    // ── DAO ──
    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()
    private val historyDao = database.watchHistoryDao()
    val categoryDao = database.categoryDao()

    // ── DataStore ──
    private val settingsDataStore = SettingsDataStore(context)

    // ── Mapper ──
    private val playlistMapper = PlaylistMapper()
    private val channelMapper = ChannelMapper()
    private val historyMapper = HistoryMapper()

    // ── Parser + Network ──
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .retryOnConnectionFailure(true)
        .build()

    private val m3uParser = M3UParser()

    private val xstreamParser = XstreamParser()

    // ── Repository ──
    private val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl(
        context        = context,
        playlistDao    = playlistDao,
        channelDao     = channelDao,
        categoryDao    = categoryDao,
        playlistMapper = playlistMapper,
        channelMapper  = channelMapper,
        m3uParser      = m3uParser,
        xstreamParser  = xstreamParser
        )

    val channelRepository: ChannelRepository = ChannelRepositoryImpl(
        channelDao    = channelDao,
        channelMapper = channelMapper
    )

    private val historyRepository: WatchHistoryRepository = WatchHistoryRepositoryImpl(
        historyDao    = historyDao,
        historyMapper = historyMapper
    )

    // ── UseCase: Playlist ──
    val getPlaylistsUseCase    = GetPlaylistsUseCase(playlistRepository)
    val addPlaylistUseCase     = AddPlaylistUseCase(playlistRepository)
    val deletePlaylistUseCase  = DeletePlaylistUseCase(playlistRepository)

    val addXstreamUseCase = AddXstreamUseCase(playlistRepository)

    // ── UseCase: Channel ──
    val getChannelsUseCase = GetChannelsUseCase(channelRepository)

    // ── UseCase: History ──
    val addWatchHistoryUseCase    = AddWatchHistoryUseCase(historyRepository)
    val getWatchHistoryUseCase    = GetWatchHistoryUseCase(historyRepository)
    val deleteWatchHistoryUseCase = DeleteWatchHistoryUseCase(historyRepository)
}