package com.example.anotheriptv.presentation.settings.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RefreshState {
    object Idle : RefreshState()
    data class Loading(val progress: Int, val status: String) : RefreshState()
    data class Success(val playlistType: String) : RefreshState()
    data class NavigateToLoading(val playlistId: Long) : RefreshState()
    data class Error(val message: String) : RefreshState()
}

class SettingsViewModel(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist

    fun loadPlaylist(id: Long) {
        viewModelScope.launch {
            _playlist.value = repository.getPlaylistById(id)
        }
    }

    fun refreshPlaylist(id: Long) {
        viewModelScope.launch {
            try {
                val type = _playlist.value?.type ?: run {
                    // Load nếu chưa có
                    repository.getPlaylistById(id)?.type ?: "M3U"
                }

                if (type == "XSTREAM") {
                    // Không xử lý ở đây, đẩy qua LoadingFragment
                    _refreshState.value = RefreshState.NavigateToLoading(id)
                    return@launch
                }

                // M3U: xử lý bình thường
                _refreshState.value = RefreshState.Loading(0, "Starting...")
                repository.refreshPlaylist(id) { progress, status ->
                    _refreshState.value = RefreshState.Loading(progress, status)
                }
                _refreshState.value = RefreshState.Success("M3U")
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetRefreshState() {
        _refreshState.value = RefreshState.Idle
    }

}