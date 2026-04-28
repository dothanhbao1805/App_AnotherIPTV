package com.example.anotheriptv.presentation.xstream.series

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentSearchSeriesBinding
import com.example.anotheriptv.presentation.xstream.ContainerXstreamActivity
import com.example.anotheriptv.presentation.xstream.series.Adapter.CategoryAdapter
import com.example.anotheriptv.presentation.xstream.series.Adapter.ItemSeriesXstreamAllAdapter
import com.example.anotheriptv.presentation.xstream.series.ViewModel.SeriesXstreamViewModel
import com.example.anotheriptv.presentation.xstream.series.ViewModelFactory.SeriesXstreamViewModelFactory
import com.example.anotheriptv.utils.GridSpacingItemDecoration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.text.contains

class SearchSeriesFragment : Fragment() {

    private var _binding: FragmentSearchSeriesBinding? = null
    private val binding get() = _binding!!

    private var playlistId: Long = -1L
    private lateinit var seriesAdapter: ItemSeriesXstreamAllAdapter
    private var searchJob: Job? = null

    private val keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        _binding?.let { b ->
            val rect = android.graphics.Rect()
            b.root.getWindowVisibleDisplayFrame(rect)

            val screenHeight = b.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val isKeyboardNowVisible = keypadHeight > screenHeight * 0.15

            updateParentMenuVisibility(!isKeyboardNowVisible)
        }
    }


    private val viewModel: SeriesXstreamViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        SeriesXstreamViewModelFactory(
            container.channelRepository,
            container.categoryDao,
            container.addWatchHistoryUseCase,
            container.channelDao,
            container.xstreamParser
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)

        playlistId = arguments?.getLong("playlistId") ?: -1L

        setupRecyclerView()
        setupSearchLogic()

        binding.ivClearSearch.setOnClickListener {
            closeKeyboard()
            parentFragmentManager.popBackStack()
        }

    }

    private fun setupRecyclerView() {
        seriesAdapter = ItemSeriesXstreamAllAdapter { channel ->
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    SeriesDetailFragment.newInstance(
                        channel    = channel,
                        playlistId = playlistId
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerSearch.apply {
            adapter = seriesAdapter
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
                filterMovies(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterMovies(query: String) {
        searchJob?.cancel()

        if (query.length < 2) {
            seriesAdapter.submitList(emptyList())
            binding.layoutEmpty.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerSearch.visibility = View.GONE

        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(150)

            val allLive = viewModel.allSeries.value
            val filtered = withContext(Dispatchers.Default) {
                allLive.filter { channel ->
                    channel.name.contains(query, ignoreCase = true) ||
                            channel.category.contains(query, ignoreCase = true) ||
                            channel.genre?.contains(query, ignoreCase = true) == true
                }.take(60)
            }

            if (isActive) {
                binding.progressBar.visibility = View.GONE
                binding.recyclerSearch.visibility = View.VISIBLE
                seriesAdapter.submitList(filtered)
                binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateParentMenuVisibility(show: Boolean) {
        (requireActivity() as? ContainerXstreamActivity)?.let { activity ->
            // Giả sử thanh menu của bạn có ID là bottom_navigation_view
            val menu = activity.findViewById<View>(R.id.bottom_navigation)
            menu?.visibility = if (show) View.VISIBLE else View.GONE
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
        view?.viewTreeObserver?.removeOnGlobalLayoutListener(keyboardLayoutListener)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(playlistId: Long) = SearchSeriesFragment().apply {
            arguments = Bundle().apply {
                putLong("playlistId", playlistId)
            }
        }
    }

}