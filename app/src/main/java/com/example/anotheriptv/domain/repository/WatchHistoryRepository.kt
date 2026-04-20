package com.example.anotheriptv.domain.repository

import com.example.anotheriptv.domain.model.WatchHistory
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getWatchHistory(): Flow<List<WatchHistory>>
    fun getWatchHistoryByPlaylist(playlistId: Long): Flow<List<WatchHistory>>
    suspend fun upsertHistory(watchHistory: WatchHistory)
    suspend fun deleteHistoryById(id: Long)

}