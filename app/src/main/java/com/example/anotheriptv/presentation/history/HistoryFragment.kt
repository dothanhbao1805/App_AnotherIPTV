package com.example.anotheriptv.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.data.local.entity.HistoryWithUrl
import com.example.anotheriptv.databinding.FragmentHistoryBinding
import com.example.anotheriptv.presentation.history.Adapter.HistoryAdapter
import com.example.anotheriptv.presentation.history.ViewModel.HistoryViewModel
import com.example.anotheriptv.presentation.history.ViewModelFactory.HistoryViewModelFactory
import com.example.anotheriptv.presentation.player.m3u.PlayerActivity
import com.example.anotheriptv.presentation.player.xstream.PlayerLiveXstreamActivity
import com.example.anotheriptv.presentation.player.xstream.PlayerMoviesXstreamActivity
import com.example.anotheriptv.presentation.xstream.movie.DetailMovieActivity
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

    private val viewModel: HistoryViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        HistoryViewModelFactory(
            container.getWatchHistoryUseCase,
            container.deleteWatchHistoryUseCase,
            container.channelRepository
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
                lifecycleScope.launch(Dispatchers.IO) {
                    val container = (requireActivity().application as MyApp).container
                    // Dùng channelId thay vì streamUrl để tránh bug series
                    container.channelDao.updateFavorite(historyItem.channelId, false)
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
        if (historyItem.contentType != "SERIES" && historyItem.streamUrl.isNullOrEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Không tìm thấy URL stream", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        when (historyItem.contentType) {
            "LIVE" -> startActivity(android.content.Intent(requireContext(), PlayerLiveXstreamActivity::class.java).apply {
                putExtra("channelName", historyItem.channelName)
                putExtra("streamUrl",   historyItem.streamUrl)
                putExtra("playlistId",  playlistId)
                putExtra("contentType", historyItem.contentType)
            })

            "MOVIE" -> startActivity(android.content.Intent(requireContext(), DetailMovieActivity::class.java).apply {
                putExtra(DetailMovieActivity.EXTRA_NAME,         historyItem.channelName)
                putExtra(DetailMovieActivity.EXTRA_LOGO,         historyItem.channelLogo ?: "")
                putExtra(DetailMovieActivity.EXTRA_STREAM_URL,   historyItem.streamUrl ?: "")
                putExtra(DetailMovieActivity.EXTRA_PLAYLIST_ID,  playlistId)
                putExtra(DetailMovieActivity.EXTRA_CHANNEL_ID,   historyItem.channelId)
                putExtra(DetailMovieActivity.EXTRA_RATING,       historyItem.rating)
                putExtra(DetailMovieActivity.EXTRA_STREAM_ID,    historyItem.streamId)
                putExtra(DetailMovieActivity.EXTRA_RELEASE_DATE, historyItem.releaseDate)
            })

            "SERIES" -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val container = (requireActivity().application as MyApp).container
                    val channel = container.channelRepository.getChannelById(historyItem.channelId)
                        ?: return@launch

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        parentFragmentManager.beginTransaction()
                            .replace(
                                R.id.fragment_container,
                                com.example.anotheriptv.presentation.xstream.series.SeriesDetailFragment.newInstance(
                                    channel    = channel,
                                    playlistId = playlistId
                                )
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }

            else -> startActivity(android.content.Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("channelName", historyItem.channelName)
                putExtra("streamUrl",   historyItem.streamUrl)
            })
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
                kotlinx.coroutines.flow.combine(
                    viewModel.historyChannels,
                    viewModel.favoriteChannels
                ) { historyList, favoriteChannels ->
                    Pair(historyList, favoriteChannels)
                }.collect { (historyList, favoriteChannels) ->

                    // Update history sections
                    updateSection(binding.layoutLive,   liveAdapter,   historyList.filter { it.contentType == "LIVE" })
                    updateSection(binding.layoutMovie,  movieAdapter,  historyList.filter { it.contentType == "MOVIE" })
                    updateSection(binding.layoutSeries, seriesAdapter, historyList.filter { it.contentType == "SERIES" })

                    // Update favorites
                    if (favoriteChannels.isEmpty()) {
                        binding.layoutFavorite.visibility = View.GONE
                    } else {
                        binding.layoutFavorite.visibility = View.VISIBLE
                        val favoriteList = favoriteChannels.map { channel ->
                            HistoryWithUrl(
                                historyId   = channel.id,
                                channelId   = channel.id,
                                playlistId  = -1L,
                                channelName = channel.name,
                                channelLogo = channel.logo,
                                streamUrl   = channel.url,
                                watchedAt   = 0L,
                                contentType = channel.contentType,
                                isFavorite  = true,
                                rating      = channel.rating ?: 0f,
                                streamId    = channel.id.toString(),
                                releaseDate = channel.releaseDate ?: ""
                            )
                        }
                        favoriteAdapter.submitList(favoriteList)
                    }

                    // Update empty state — cả 2 đã có giá trị mới nhất
                    val isEmpty = historyList.isEmpty() && favoriteChannels.isEmpty()
                    binding.layoutEmpty.visibility      = if (isEmpty) View.VISIBLE else View.GONE
                    binding.scrollViewContent.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
                        viewModel.loadHistory(playlistId)
                        true
                    }
                    R.id.action_clear_all -> {
                        showClearAllDialog()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun showClearAllDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_clear_all, null)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemove).setOnClickListener {
            viewModel.clearAllHistory(playlistId)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}