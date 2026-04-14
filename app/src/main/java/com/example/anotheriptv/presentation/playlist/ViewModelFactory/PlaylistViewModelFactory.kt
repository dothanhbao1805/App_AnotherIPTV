package com.example.anotheriptv.presentation.playlist.ViewModelFactory


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.anotheriptv.domain.usecase.playlist.AddPlaylistUseCase
import com.example.anotheriptv.domain.usecase.playlist.DeletePlaylistUseCase
import com.example.anotheriptv.domain.usecase.playlist.GetPlaylistsUseCase
import com.example.anotheriptv.presentation.playlist.ViewModel.PlaylistViewModel

class PlaylistViewModelFactory(
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addPlaylistUseCase: AddPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaylistViewModel(
                getPlaylistsUseCase,
                addPlaylistUseCase,
                deletePlaylistUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}