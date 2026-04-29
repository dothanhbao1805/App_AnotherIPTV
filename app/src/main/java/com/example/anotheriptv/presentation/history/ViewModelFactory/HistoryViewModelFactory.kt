package com.example.anotheriptv.presentation.history.ViewModelFactory


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.usecase.history.DeleteWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.history.GetWatchHistoryUseCase
import com.example.anotheriptv.presentation.history.ViewModel.HistoryViewModel

class HistoryViewModelFactory(
    private val getWatchHistoryUseCase: GetWatchHistoryUseCase,
    private val deleteWatchHistoryUseCase: DeleteWatchHistoryUseCase,
    private val channelRepository: ChannelRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(
                getWatchHistoryUseCase,
                deleteWatchHistoryUseCase,
                channelRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}