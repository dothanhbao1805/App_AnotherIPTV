package com.example.anotheriptv.presentation.channels.ViewModelFactory


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.anotheriptv.domain.usecase.channel.GetChannelsUseCase
import com.example.anotheriptv.presentation.channels.ViewModel.ChannelViewModel

class ChannelViewModelFactory(
    private val getChannelsUseCase: GetChannelsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChannelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChannelViewModel(getChannelsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}