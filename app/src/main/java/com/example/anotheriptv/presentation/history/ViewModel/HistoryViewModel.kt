package com.example.anotheriptv.presentation.history.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.data.local.entity.HistoryWithUrl
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.usecase.history.DeleteWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.history.GetWatchHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach


class HistoryViewModel(
    private val getWatchHistoryUseCase: GetWatchHistoryUseCase,
    private val deleteWatchHistoryUseCase: DeleteWatchHistoryUseCase,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _playlistId = MutableStateFlow(-1L)
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val historyChannels: StateFlow<List<HistoryWithUrl>> = _playlistId
        .flatMapLatest { id ->
            getWatchHistoryUseCase(id)
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteChannels: StateFlow<List<Channel>> = _playlistId
        .flatMapLatest { id -> channelRepository.getFavoriteChannels(id) }  // ← truyền id
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadHistory(playlistId: Long) {
        _isLoading.value = true
        _playlistId.value = playlistId
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            deleteWatchHistoryUseCase(id)
        }
    }

    fun clearAllHistory(playlistId: Long) {
        viewModelScope.launch {
            deleteWatchHistoryUseCase.deleteAll(playlistId)
        }
    }

}