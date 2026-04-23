package com.example.anotheriptv.presentation.xstream.movie.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class MovieXstreamViewModel(
    private val channelRepository: ChannelRepository,
    private val categoryDao: CategoryDao,
    private val addWatchHistoryUseCase: AddWatchHistoryUseCase
) : ViewModel() {

    private val _categoriesWithChannels = MutableStateFlow<List<CategoryWithChannels>>(emptyList())
    val categoriesWithChannels: StateFlow<List<CategoryWithChannels>> = _categoriesWithChannels.asStateFlow()

    fun loadLiveChannels(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = categoryDao.getCategoriesByPlaylistAndType(playlistId, "MOVIE")
            val result = categories.map { cat ->
                val channels = channelRepository.getChannelsByCategoryLimit10(
                    playlistId  = playlistId,
                    contentType = "MOVIE",
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

}