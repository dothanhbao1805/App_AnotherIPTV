package com.example.anotheriptv.presentation.xstream.live

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.databinding.FragmentLiveXstreamAllBinding
import com.example.anotheriptv.presentation.player.PlayerActivity
import com.example.anotheriptv.presentation.xstream.live.Adapter.ItemLiveXstreamAllAdapter
import com.example.anotheriptv.presentation.xstream.live.ViewModel.LiveXstreamAllViewModel
import com.example.anotheriptv.presentation.xstream.live.ViewModel.LiveXstreamAllViewModelFactory
import com.example.anotheriptv.utils.GridSpacingItemDecoration
import kotlinx.coroutines.launch

class LiveXstreamAllFragment : Fragment() {

    private var _binding: FragmentLiveXstreamAllBinding? = null
    private val binding get() = _binding!!

    private lateinit var channelAdapter: ItemLiveXstreamAllAdapter

    // Lấy arguments từ Bundle
    private val playlistId   by lazy { arguments?.getLong("playlistId", -1L) ?: -1L }
    private val contentType  by lazy { arguments?.getString("contentType", "LIVE") ?: "LIVE" }
    private val categoryId   by lazy { arguments?.getString("categoryId", "") ?: "" }
    private val categoryName by lazy { arguments?.getString("categoryName", "") ?: "" }

    private val viewModel: LiveXstreamAllViewModel by viewModels {
        val container = (requireActivity().application as MyApp).container
        LiveXstreamAllViewModelFactory(
            container.getChannelsByCategoryUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveXstreamAllBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitleCategory.text = categoryName

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        observeChannels()

        // Load data
        viewModel.loadChannels(playlistId, contentType, categoryId)
    }

    private fun setupToolbar() {
        binding.tvTitleCategory.text = categoryName.ifBlank { "Channels" }
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.ivFilter.visibility = View.GONE  // ẩn filter nếu chưa dùng
    }

    private fun setupRecyclerView() {
        channelAdapter = ItemLiveXstreamAllAdapter { channel ->
            startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("channelName", channel.name)
                putExtra("streamUrl", channel.url)
            })
        }

        binding.recyclerChannels.apply {
            adapter = channelAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
            addItemDecoration(GridSpacingItemDecoration(spanCount = 3, spacing = 8))
        }
    }

    private fun setupSearch() {
        binding.ivSearch.setOnClickListener {
            val isVisible = binding.layoutSearch.visibility == View.VISIBLE
            binding.layoutSearch.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) binding.etSearch.requestFocus()
            else {
                binding.etSearch.setText("")
                viewModel.clearSearch()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                binding.ivClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.search(query)
            }
        })

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            viewModel.clearSearch()
        }
    }

    private fun observeChannels() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.channels.collect { channels ->
                        channelAdapter.submitList(channels)
                        binding.layoutEmpty.visibility =
                            if (channels.isEmpty() && viewModel.isLoading.value == false)
                                View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            playlistId: Long,
            contentType: String,
            categoryId: String,
            categoryName: String
        ) = LiveXstreamAllFragment().apply {
            arguments = Bundle().apply {
                putLong("playlistId", playlistId)
                putString("contentType", contentType)
                putString("categoryId", categoryId)
                putString("categoryName", categoryName)
            }
        }
    }

}
