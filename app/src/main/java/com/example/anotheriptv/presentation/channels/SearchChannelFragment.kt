package com.example.anotheriptv.presentation.channels

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.databinding.FragmentSearchChannelBinding
import com.example.anotheriptv.presentation.channels.Adapter.ChannelAdapter
import com.example.anotheriptv.presentation.channels.ViewModel.ChannelViewModel
import com.example.anotheriptv.presentation.channels.ViewModelFactory.ChannelViewModelFactory
import com.example.anotheriptv.presentation.player.PlayerActivity
import kotlinx.coroutines.launch

class SearchChannelFragment : Fragment() {

    private var _binding: FragmentSearchChannelBinding? = null
    private val binding get() = _binding!!
    private var playlistId: Long = -1L

    private lateinit var channelAdapter: ChannelAdapter

    private val viewModel: ChannelViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        ChannelViewModelFactory(
            container.getChannelsUseCase,
            container.addWatchHistoryUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchChannelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistId = arguments?.getLong("playlistId") ?: -1L

        setupRecyclerView()
        setupSearchLogic()
        observeData()

        binding.ivClearSearch.setOnClickListener {
            closeKeyboard()
            parentFragmentManager.popBackStack()
        }

    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter { channel ->
            viewModel.addToHistory(
                channelId = channel.id,
                playlistId = playlistId,
                channelName = channel.name,
                channelLogo = channel.logo
            )
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("channelName", channel.name)
                putExtra("streamUrl", channel.url)
            }
            startActivity(intent)
        }

        binding.recyclerSearch.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = channelAdapter
        }
    }


    private fun setupSearchLogic() {
        binding.etSearch.requestFocus()
        showKeyboard()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()

                // Hiển thị/Ẩn nút X dựa trên việc có text hay không
                binding.ivClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

                // THỰC HIỆN LỌC NGAY LẬP TỨC (Real-time)
                filterChannels(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeData() {
        // Khi vừa vào trang, hiển thị mặc định toàn bộ danh sách đang có trong ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.channels.collect { allChannels ->
                    // Chỉ cập nhật nếu người dùng chưa bắt đầu nhập gì (để hiện default "All")
                    if (binding.etSearch.text.isEmpty()) {
                        channelAdapter.submitList(allChannels)
                    }
                }
            }
        }
    }

    private fun filterChannels(query: String) {

        Log.d("SEARCH_DEBUG", "Đang gõ: '$query'")
        val allChannels = viewModel.channels.value

        Log.d("SEARCH_DEBUG", "Tổng số kênh lấy từ ViewModel: ${allChannels.size}")
        val filteredList = if (query.isEmpty()) {
            allChannels
        } else {
            allChannels.filter { channel ->
                channel.name.contains(query, ignoreCase = true) ||
                        channel.category.contains(query, ignoreCase = true)
            }
        }

        Log.d("SEARCH_DEBUG", "Số kênh sau khi lọc: ${filteredList.size}")
        channelAdapter.submitList(filteredList)

        binding.tvNoResult.visibility = if (filteredList.isEmpty() && query.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showKeyboard() {
        binding.etSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun closeKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}