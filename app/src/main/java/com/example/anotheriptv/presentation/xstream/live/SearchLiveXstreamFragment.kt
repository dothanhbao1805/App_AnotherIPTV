package com.example.anotheriptv.presentation.xstream.live

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.databinding.FragmentSearchLiveXstreamBinding
import com.example.anotheriptv.presentation.xstream.live.Adapter.ItemLiveXstreamAllAdapter
import com.example.anotheriptv.presentation.xstream.live.ViewModel.LiveXstreamViewModel
import com.example.anotheriptv.presentation.xstream.live.ViewModelFactory.LiveXstreamViewModelFactory
import com.example.anotheriptv.utils.GridSpacingItemDecoration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SearchLiveXstreamFragment : Fragment() {

    private var _binding: FragmentSearchLiveXstreamBinding? = null
    private val binding get() = _binding!!

    private var playlistId: Long = -1L
    private lateinit var liveAdapter: ItemLiveXstreamAllAdapter

    private var searchJob: Job? = null

    private val viewModel: LiveXstreamViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        LiveXstreamViewModelFactory(
            container.channelRepository,
            container.categoryDao,
            container.addWatchHistoryUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchLiveXstreamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong("playlistId") ?: -1L

        setupRecyclerView()
        setupSearchLogic()
        binding.ivClearSearch.setOnClickListener {
            closeKeyboard()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        liveAdapter = ItemLiveXstreamAllAdapter { channel ->
            val intent = android.content.Intent(requireContext(), com.example.anotheriptv.presentation.player.PlayerActivity::class.java).apply {
                putExtra("channelName", channel.name)
                putExtra("streamUrl", channel.url)
            }
            startActivity(intent)
        }
        binding.recyclerSearch.apply {
            adapter = liveAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
            addItemDecoration(GridSpacingItemDecoration(spanCount = 3, spacing = 8))
        }
    }

    private fun setupSearchLogic() {
        binding.etSearch.requestFocus()
        showKeyboard()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                binding.ivClearSearch.visibility =
                    if (query.isEmpty()) View.GONE else View.VISIBLE
                filterLive(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun filterLive(query: String) {
        searchJob?.cancel()

        if (query.length < 2) {
            liveAdapter.submitList(emptyList())
            binding.layoutEmpty.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerSearch.visibility = View.GONE

        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(150)

            val allLive = viewModel.allLive.value
            val filtered = withContext(Dispatchers.Default) {
                allLive.filter { channel ->
                    channel.name.contains(query, ignoreCase = true) ||
                            channel.category.contains(query, ignoreCase = true) ||
                            channel.genre?.contains(query, ignoreCase = true) == true
                }.take(60)
            }

            if (isActive) {
                // Ẩn progress, hiện kết quả
                binding.progressBar.visibility = View.GONE
                binding.recyclerSearch.visibility = View.VISIBLE
                liveAdapter.submitList(filtered)
                binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showKeyboard() {
        binding.etSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun closeKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(playlistId: Long) = SearchLiveXstreamFragment().apply {
            arguments = Bundle().apply {
                putLong("playlistId", playlistId)
            }
        }
    }

}