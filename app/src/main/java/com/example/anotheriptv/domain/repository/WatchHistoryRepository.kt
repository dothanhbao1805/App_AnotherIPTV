package com.example.anotheriptv.domain.repository

import com.example.anotheriptv.domain.model.WatchHistory
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getWatchHistory(): Flow<List<WatchHistory>>
    suspend fun addHistory(watchHistory: WatchHistory)
    suspend fun deleteHistoryById(id: Long)
}