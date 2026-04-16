package com.example.anotheriptv.presentation.channels.ViewModelFactory


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.anotheriptv.domain.usecase.channel.GetChannelsUseCase
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import com.example.anotheriptv.presentation.channels.ViewModel.ChannelViewModel

class ChannelViewModelFactory(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val addWatchHistoryUseCase: AddWatchHistoryUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChannelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChannelViewModel(getChannelsUseCase, addWatchHistoryUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}