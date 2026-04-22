package com.example.anotheriptv.presentation.history.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.data.local.entity.HistoryWithUrl
import com.example.anotheriptv.domain.usecase.history.DeleteWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.history.GetWatchHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val getWatchHistoryUseCase: GetWatchHistoryUseCase,
    private val deleteWatchHistoryUseCase: DeleteWatchHistoryUseCase
) : ViewModel() {

    private val _playlistId = MutableStateFlow(-1L)

    val historyChannels: StateFlow<List<HistoryWithUrl>> = _playlistId
        .flatMapLatest { id ->
            getWatchHistoryUseCase(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadHistory(playlistId: Long) {
        _playlistId.value = playlistId
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            deleteWatchHistoryUseCase(id)
        }
    }

    // Thêm hàm xóa tất cả lịch sử nếu bạn định làm tính năng Clear All
    fun clearAllHistory(playlistId: Long) {
        viewModelScope.launch {
            // Gọi UseCase xóa nếu bạn đã tạo, hoặc gọi trực tiếp repo
        }
    }
}