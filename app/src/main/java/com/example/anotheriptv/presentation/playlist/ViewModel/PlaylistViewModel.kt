package com.example.anotheriptv.presentation.playlist.ViewModel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.domain.usecase.playlist.AddPlaylistUseCase
import com.example.anotheriptv.domain.usecase.playlist.DeletePlaylistUseCase
import com.example.anotheriptv.domain.usecase.playlist.GetPlaylistsUseCase
import com.example.anotheriptv.presentation.playlist.UiState.PlaylistUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addPlaylistUseCase: AddPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Idle)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    val playlists = getPlaylistsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading
            addPlaylistUseCase(playlist)
                .onSuccess { _uiState.value = PlaylistUiState.Success }
                .onFailure { _uiState.value = PlaylistUiState.Error(it.message ?: "Lỗi không xác định") }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading
            deletePlaylistUseCase(id)
                .onSuccess { _uiState.value = PlaylistUiState.Success }
                .onFailure { _uiState.value = PlaylistUiState.Error(it.message ?: "Xóa thất bại") }
        }
    }

    fun resetState() {
        _uiState.value = PlaylistUiState.Idle
    }
}