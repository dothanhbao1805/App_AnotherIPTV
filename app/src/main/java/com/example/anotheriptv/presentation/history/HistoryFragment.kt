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

    private lateinit var historyAdapter: HistoryAdapter

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
        setupRecyclerView()
        setupMenu()

        // 2. Gọi hàm lắng nghe dữ liệu
        observeViewModel()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { historyItem ->
                // 3. Mở Player khi click vào item lịch sử (Sử dụng streamUrl đã được JOIN)
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

        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL, // Cuộn ngang
                false
            )
            adapter = historyAdapter
        }
    }

    private fun showDeleteConfirmationDialog(historyItem: WatchHistory) {

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

            viewModel.deleteHistory(historyItem.id)
            dialog.dismiss()

        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 5. Lắng nghe StateFlow từ ViewModel
                viewModel.historyChannels.collect { historyList ->
                    Log.d("DEBUG_HISTORY", "Đã lấy được ${historyList.size} kênh lịch sử")

                    historyAdapter.submitList(historyList)

                }
            }
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