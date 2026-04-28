package com.example.anotheriptv.presentation.xstream.series.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.data.local.dao.ChannelDao
import com.example.anotheriptv.data.remote.parser.XstreamParser
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import com.example.anotheriptv.presentation.xstream.series.ViewModel.SeriesXstreamViewModel

class SeriesXstreamViewModelFactory(
    private val channelRepository: ChannelRepository,
    private val categoryDao: CategoryDao,
    private val addWatchHistoryUseCase: AddWatchHistoryUseCase,
    private val channelDao: ChannelDao,
    private val xstreamParser: XstreamParser
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeriesXstreamViewModel::class.java)) {
            return SeriesXstreamViewModel(
                channelRepository    = channelRepository,
                categoryDao          = categoryDao,
                addWatchHistoryUseCase = addWatchHistoryUseCase,
                channelDao           = channelDao,
                xstreamParser        = xstreamParser
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}