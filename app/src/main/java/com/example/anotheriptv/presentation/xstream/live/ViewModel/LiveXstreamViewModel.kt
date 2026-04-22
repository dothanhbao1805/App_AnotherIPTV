package com.example.anotheriptv.presentation.xstream.live.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class LiveXstreamViewModel(
    private val channelRepository: ChannelRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _categoriesWithChannels = MutableStateFlow<List<CategoryWithChannels>>(emptyList())
    val categoriesWithChannels: StateFlow<List<CategoryWithChannels>> = _categoriesWithChannels.asStateFlow()

    fun loadLiveChannels(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = categoryDao.getCategoriesByPlaylistAndType(playlistId, "LIVE")
            val result = categories.map { cat ->
                val channels = channelRepository.getChannelsByCategoryLimit10(
                    playlistId  = playlistId,
                    contentType = "LIVE",
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
}