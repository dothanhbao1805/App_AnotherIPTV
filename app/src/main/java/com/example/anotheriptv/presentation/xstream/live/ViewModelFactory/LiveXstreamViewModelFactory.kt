package com.example.anotheriptv.presentation.xstream.live.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.presentation.xstream.live.ViewModel.LiveXstreamViewModel

class LiveXstreamViewModelFactory(
    private val channelRepository: ChannelRepository,
    private val categoryDao: CategoryDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LiveXstreamViewModel::class.java)) {
            return LiveXstreamViewModel(channelRepository, categoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}