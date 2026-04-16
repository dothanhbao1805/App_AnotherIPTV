package com.example.anotheriptv.domain.usecase.history

import com.example.anotheriptv.domain.repository.WatchHistoryRepository

class DeleteWatchHistoryUseCase(private val repository: WatchHistoryRepository) {
    suspend operator fun invoke(id: Long) {
        repository.deleteHistoryById(id)
    }
}