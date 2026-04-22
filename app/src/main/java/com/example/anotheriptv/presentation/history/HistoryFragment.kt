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
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private var playlistId: Long = -1L

    private lateinit var historyAdapter: HistoryAdapter

    private lateinit var liveAdapter: HistoryAdapter
    private lateinit var movieAdapter: HistoryAdapter
    private lateinit var seriesAdapter: HistoryAdapter

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

        fun createHistoryAdapter() = HistoryAdapter(
            onItemClick = { historyItem ->
                if (historyItem.streamUrl.isNullOrEmpty()) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Không tìm thấy URL stream",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@HistoryAdapter
                }
                val intent = android.content.Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("channelName", historyItem.channelName)
                    putExtra("streamUrl", historyItem.streamUrl)
                }
                startActivity(intent)
            },
            onRemoveClick = { historyItem ->
                showDeleteConfirmationDialog(historyItem)
            }
        )

        liveAdapter = createHistoryAdapter()
        movieAdapter = createHistoryAdapter()
        seriesAdapter = createHistoryAdapter()

        binding.recyclerLive.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = liveAdapter
        }

        binding.recyclerMovie.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = movieAdapter
        }

        binding.recyclerSeries.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = seriesAdapter
        }

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
                // Lắng nghe dữ liệu trả về từ Flow<List<HistoryWithUrl>>
                viewModel.historyChannels.collect { historyList ->
                    Log.d("DEBUG_HISTORY", "Nhận được: ${historyList.size} mục lịch sử")

                    // 1. Phân loại cho Live Streams
                    val liveList = historyList.filter { it.contentType == "LIVE" }
                    updateSection(binding.layoutLive, liveAdapter, liveList)

                    // 2. Phân loại cho Movies
                    val movieList = historyList.filter { it.contentType == "MOVIE" }
                    updateSection(binding.layoutMovie, movieAdapter, movieList)

                    // 3. Phân loại cho Series
                    val seriesList = historyList.filter { it.contentType == "SERIES" }
                    updateSection(binding.layoutSeries, seriesAdapter, seriesList)

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
                        // Vì dùng Flow, dữ liệu tự động refresh rồi nên không cần làm gì ở đây,
                        // hoặc bạn có thể tạo 1 hiệu ứng Toast nhẹ báo "Đã làm mới"
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