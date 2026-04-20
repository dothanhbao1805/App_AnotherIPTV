package com.example.anotheriptv.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.anotheriptv.data.local.entity.HistoryWithUrl
import com.example.anotheriptv.data.local.entity.PlaylistEntity
import com.example.anotheriptv.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface WatchHistoryDao {

    @Query("""
        SELECT 
            h.id as historyId,
            h.channelId,
            h.playlistId,
            h.channelName,
            h.channelLogo,
            c.url as streamUrl,
            h.watchedAt
        FROM watch_history h
        LEFT JOIN channels c ON h.channelId = c.id
        ORDER BY h.watchedAt DESC
    """)
    fun getAll(): Flow<List<HistoryWithUrl>>

    @Query("""
        SELECT 
            h.id as historyId,
            h.channelId,
            h.playlistId,
            h.channelName,
            h.channelLogo,
            c.url as streamUrl,
            h.watchedAt
        FROM watch_history h
        LEFT JOIN channels c ON h.channelId = c.id
        WHERE h.playlistId = :playlistId
        ORDER BY h.watchedAt DESC
    """)
    fun getByPlaylistId(playlistId: Long): Flow<List<HistoryWithUrl>>

    @Query("DELETE FROM watch_history WHERE channelId = :channelId AND playlistId = :playlistId")
    suspend fun deleteExisting(channelId: Long, playlistId: Long)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insert(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
