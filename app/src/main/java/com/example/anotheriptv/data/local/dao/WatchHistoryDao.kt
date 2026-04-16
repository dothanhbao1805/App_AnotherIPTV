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

    @Query("DELETE FROM watch_history WHERE channelId = :channelId")
    suspend fun removeOldHistoryByChannelId(channelId: Long)

    @Insert
    suspend fun insertHistory(history: WatchHistoryEntity)

    @Transaction
    suspend fun upsertHistory(history: WatchHistoryEntity) {
        removeOldHistoryByChannelId(history.channelId)
        insertHistory(history)
    }

    @Query("""
        SELECT h.id as historyId, h.channelId, h.channelName, h.channelLogo, c.url as streamUrl, h.watchedAt 
        FROM watch_history h 
        INNER JOIN channels c ON h.channelId = c.id 
        ORDER BY h.watchedAt DESC
    """)
    fun getHistoryWithUrl(): Flow<List<HistoryWithUrl>>

    @Query("DELETE FROM watch_history WHERE id = :historyId")
    suspend fun deleteHistoryById(historyId: Long)


}
