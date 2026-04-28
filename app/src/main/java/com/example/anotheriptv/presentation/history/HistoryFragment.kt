package com.example.anotheriptv.presentation.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.data.local.entity.HistoryWithUrl
import com.example.anotheriptv.databinding.FragmentHistoryBinding
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.presentation.history.Adapter.HistoryAdapter
import com.example.anotheriptv.presentation.history.ViewModel.HistoryViewModel
import com.example.anotheriptv.presentation.history.ViewModelFactory.HistoryViewModelFactory
import com.example.anotheriptv.presentation.player.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private var playlistId: Long = -1L

    private lateinit var historyAdapter: HistoryAdapter

    private lateinit var liveAdapter: HistoryAdapter
    private lateinit var movieAdapter: HistoryAdapter
    private lateinit var seriesAdapter: HistoryAdapter
    private lateinit var favoriteAdapter: HistoryAdapter

    private val viewModel: HistoryViewModel by viewModels {
        val container = (requireActivity().application as MyApp).container
        HistoryViewModelFactory(
            container.getWatchHistoryUseCase,
            container.deleteWatchHistoryUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong("playlistId") ?: -1L

        setupRecyclerView()
        setupMenu()
        observeViewModel()

        viewModel.loadHistory(playlistId)
    }

    private fun setupRecyclerView() {

        // 1. Hàm tạo Adapter cho các danh sách bình thường (Live, Movie, Series)
        fun createNormalHistoryAdapter() = HistoryAdapter(
            onItemClick = { historyItem ->
                playStream(historyItem)
            },
            onRemoveClick = { historyItem ->
                showDeleteConfirmationDialog(historyItem)
            },
            onFavoriteClick = null // Không dùng tính năng bỏ thích ở danh sách thường
        )

        // 2. Khởi tạo riêng cho Favorite Adapter với logic bỏ yêu thích
        favoriteAdapter = HistoryAdapter(
            onItemClick = { historyItem ->
                playStream(historyItem)
            },
            onRemoveClick = { /* Không dùng */ },
            onFavoriteClick = { historyItem ->
                // Thực hiện bỏ yêu thích khi nhấn vào icon tim đỏ
                lifecycleScope.launch(Dispatchers.IO) {
                    val container = (requireActivity().application as MyApp).container
                    container.channelRepository.updateFavoriteStatus(historyItem.streamUrl ?: "", false)

                    // Lưu ý: Do dùng Flow, danh sách sẽ tự cập nhật và mục này tự biến mất
                }
            }
        )

        // Khởi tạo các adapter còn lại
        liveAdapter = createNormalHistoryAdapter()
        movieAdapter = createNormalHistoryAdapter()
        seriesAdapter = createNormalHistoryAdapter()

        // 3. Gán Adapter và LayoutManager cho từng RecyclerView
        binding.recyclerFavorite.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = favoriteAdapter
            setHasFixedSize(true)
        }

        binding.recyclerLive.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = liveAdapter
            setHasFixedSize(true)
        }

        binding.recyclerMovie.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = movieAdapter
            setHasFixedSize(true)
        }

        binding.recyclerSeries.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = seriesAdapter
            setHasFixedSize(true)
        }
    }

    private fun playStream(historyItem: HistoryWithUrl) {
        if (historyItem.streamUrl.isNullOrEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Không tìm thấy URL stream", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val intent = android.content.Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("channelName", historyItem.channelName)
            putExtra("streamUrl", historyItem.streamUrl)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmationDialog(historyItem: HistoryWithUrl) {

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_remove_history, null)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnRemove = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemove)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnRemove.setOnClickListener {

            viewModel.deleteHistory(historyItem.historyId)
            dialog.dismiss()

        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyChannels.collect { historyList ->
                    // Nếu danh sách tổng rỗng -> hiện Empty Layout
                    if (historyList.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.scrollViewContent.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.scrollViewContent.visibility = View.VISIBLE

                        // Lọc mục Favorite (Những mục nào có isFavorite == true)
                        val favoriteList = historyList.filter { it.isFavorite }.distinctBy { it.channelId }
                        updateSection(binding.layoutFavorite, favoriteAdapter, favoriteList)

                        // Lọc các mục khác dựa trên contentType
                        updateSection(binding.layoutLive, liveAdapter, historyList.filter { it.contentType == "LIVE" })
                        updateSection(binding.layoutMovie, movieAdapter, historyList.filter { it.contentType == "MOVIE" })
                        updateSection(binding.layoutSeries, seriesAdapter, historyList.filter { it.contentType == "SERIES" })
                    }
                }

            }
        }
    }

    private fun updateSection(layout: View, adapter: HistoryAdapter, data: List<HistoryWithUrl>) {
        if (data.isEmpty()) {
            layout.visibility = View.GONE
        } else {
            layout.visibility = View.VISIBLE
            adapter.submitList(data)
        }
    }

    private fun setupMenu() {
        binding.ivMore.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(R.menu.menu_history, popup.menu)

            // Đổi màu "Clear All" thành đỏ
            popup.menu.findItem(R.id.action_clear_all)?.let { item ->
                val spannable = android.text.SpannableString(item.title)
                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(
                        androidx.core.content.ContextCompat.getColor(
                            requireContext(), R.color.red_clear
                        )
                    ),
                    0, spannable.length,
                    android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                item.title = spannable
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_refresh -> {
                        true
                    }
                    R.id.action_clear_all -> {
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}