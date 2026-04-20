package com.example.anotheriptv.presentation.history.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.usecase.history.DeleteWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.history.GetWatchHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val getWatchHistoryUseCase: GetWatchHistoryUseCase,  // ← inject qua constructor
    private val deleteWatchHistoryUseCase: DeleteWatchHistoryUseCase
) : ViewModel() {

    private val _historyChannels = MutableStateFlow<List<WatchHistory>>(emptyList())
    val historyChannels: StateFlow<List<WatchHistory>> = _historyChannels.asStateFlow()

    fun loadHistory(playlistId: Long) {
        viewModelScope.launch {
            getWatchHistoryUseCase(playlistId).collect { list ->  // ← dùng instance, không tạo mới
                _historyChannels.value = list
            }
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            deleteWatchHistoryUseCase(id)
        }
    }
}