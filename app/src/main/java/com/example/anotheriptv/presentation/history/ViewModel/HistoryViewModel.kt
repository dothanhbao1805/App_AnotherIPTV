package com.example.anotheriptv.presentation.history.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.domain.usecase.history.DeleteWatchHistoryUseCase
import com.example.anotheriptv.domain.usecase.history.GetWatchHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    getWatchHistoryUseCase: GetWatchHistoryUseCase,
    private val deleteWatchHistoryUseCase: DeleteWatchHistoryUseCase,
) : ViewModel() {

    // Lấy dữ liệu và chuyển thành StateFlow để UI tự động cập nhật
    val historyChannels = getWatchHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            deleteWatchHistoryUseCase(id)
        }
    }


}