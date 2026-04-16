package com.example.anotheriptv.domain.usecase.history


import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.repository.WatchHistoryRepository

class AddWatchHistoryUseCase(
    private val repository: WatchHistoryRepository
) {
    suspend operator fun invoke(watchHistory: WatchHistory) {
        repository.upsertHistory(watchHistory)
    }
}