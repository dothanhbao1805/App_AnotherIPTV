package com.example.anotheriptv.presentation.xstream.series.ViewModel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class SeriesXstreamViewModel(
    private val channelRepository: ChannelRepository,
    private val categoryDao: CategoryDao,
    private val addWatchHistoryUseCase: AddWatchHistoryUseCase
) : ViewModel() {

    private val _categoriesWithChannels = MutableStateFlow<List<CategoryWithChannels>>(emptyList())
    val categoriesWithChannels: StateFlow<List<CategoryWithChannels>> = _categoriesWithChannels.asStateFlow()
    private val _allSeries = MutableStateFlow<List<Channel>>(emptyList())
    val allSeries: StateFlow<List<Channel>> = _allSeries.asStateFlow()

    fun loadLiveChannels(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = categoryDao.getCategoriesByPlaylistAndType(playlistId, "SERIES")
            val result = categories.map { cat ->
                val channels = channelRepository.getChannelsByCategoryLimit10(
                    playlistId  = playlistId,
                    contentType = "SERIES",
                    categoryId  = cat.categoryId
                ).map { it }
                CategoryWithChannels(
                    categoryId   = cat.categoryId,
                    categoryName = cat.name,
                    channels     = channels
                )
            }.filter { it.channels.isNotEmpty() }

            _categoriesWithChannels.value = result
        }
    }

    fun addToHistory(channelId: Long, playlistId: Long, channelName: String, channelLogo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val historyItem = WatchHistory(
                id = 0,
                channelId = channelId,
                playlistId = playlistId,
                channelName = channelName,
                channelLogo = channelLogo,
                streamUrl = "",
                watchedAt = System.currentTimeMillis()
            )
            addWatchHistoryUseCase(historyItem)
        }
    }

    fun loadAllSeries(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelRepository.getChannelsByContentType(playlistId, "SERIES").collect { series ->
                _allSeries.value = series
            }
        }
    }

}