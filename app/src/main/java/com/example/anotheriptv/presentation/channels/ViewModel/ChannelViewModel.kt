package com.example.anotheriptv.presentation.channels.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.usecase.channel.GetChannelsUseCase
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelViewModel(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val addWatchHistoryUseCase: AddWatchHistoryUseCase
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _filteredChannels = MutableStateFlow<List<Channel>>(emptyList())
    val filteredChannels: StateFlow<List<Channel>> = _filteredChannels.asStateFlow()

    fun loadChannels(playlistId: Long) {
        viewModelScope.launch {
            getChannelsUseCase(playlistId).collect { channels ->
                _channels.value = channels

                val cats = listOf("View All") + channels
                    .map { it.category }
                    .distinct()
                    .filter { it.isNotEmpty() }
                    .sorted()
                _categories.value = cats

                _filteredChannels.value = channels
            }
        }
    }

    fun filterByCategory(category: String) {
        _filteredChannels.value = if (category == "View All") {
            _channels.value
        } else {
            _channels.value.filter { it.category == category }
        }
    }

    fun addToHistory(channelId: Long,playlistId: Long, channelName: String, channelLogo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val historyItem = WatchHistory(
                id = 0,
                channelId = channelId,
                playlistId  = playlistId,
                channelName = channelName,
                channelLogo = channelLogo,
                streamUrl = "",
                watchedAt = System.currentTimeMillis()
            )

            addWatchHistoryUseCase(historyItem)
        }
    }

}