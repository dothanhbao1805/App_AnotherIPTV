package com.example.anotheriptv.presentation.settings.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.anotheriptv.domain.repository.PlaylistRepository
import com.example.anotheriptv.presentation.settings.ViewModel.SettingsViewModel

class SettingsViewModelFactory(
    private val repository: PlaylistRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}