package com.example.anotheriptv.presentation.history


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentHistoryBinding
import com.example.anotheriptv.presentation.history.Adapter.HistoryAdapter

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter

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

        // TODO: observe ViewModel
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { channel ->
                // TODO: navigate to player
            },
            onRemoveClick = { channel ->
                // TODO: viewModel.removeFromHistory(channel)
            }
        )
        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = historyAdapter
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
                        // TODO: viewModel.refresh()
                        true
                    }
                    R.id.action_clear_all -> {
                        // TODO: viewModel.clearAll()
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