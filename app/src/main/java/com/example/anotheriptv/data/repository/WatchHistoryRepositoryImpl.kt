package com.example.anotheriptv.data.repository

import com.example.anotheriptv.data.local.dao.WatchHistoryDao
import com.example.anotheriptv.data.mapper.HistoryMapper
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchHistoryRepositoryImpl(
    private val historyDao: WatchHistoryDao,
    private val historyMapper: HistoryMapper
) : WatchHistoryRepository {

    override fun getWatchHistory(): Flow<List<WatchHistory>> {
        // Gọi hàm getHistoryWithUrl() đã tạo trong DAO để lấy dữ liệu có chứa URL
        return historyDao.getHistoryWithUrl()
            .map { entities -> entities.map { historyMapper.toDomain(it) } }
    }

    override suspend fun upsertHistory(watchHistory: WatchHistory) {
        // Gọi hàm upsertHistory trong DAO
        historyDao.upsertHistory(historyMapper.toEntity(watchHistory))
    }

    override suspend fun deleteHistoryById(id: Long) {
        // Gọi hàm xóa theo ID
        historyDao.deleteHistoryById(id)
    }
}