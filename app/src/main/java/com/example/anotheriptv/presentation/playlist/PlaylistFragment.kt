package com.example.anotheriptv.presentation.playlist

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentPlaylistBinding
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.presentation.ContainerPlaylistActivity
import com.example.anotheriptv.presentation.playlist.Adapter.PlaylistAdapter
import com.example.anotheriptv.presentation.playlist.UiState.PlaylistUiState
import com.example.anotheriptv.presentation.playlist.ViewModel.PlaylistViewModel
import com.example.anotheriptv.presentation.playlist.ViewModelFactory.PlaylistViewModelFactory
import com.example.anotheriptv.presentation.xstream.ContainerXstreamActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class PlaylistFragment : Fragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var playlistAdapter: PlaylistAdapter

    val viewModel: PlaylistViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        PlaylistViewModelFactory(
            container.getPlaylistsUseCase,
            container.addPlaylistUseCase,
            container.deletePlaylistUseCase,
            container.addXstreamUseCase,
            container.playlistRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerPlaylists.visibility = View.GONE

        setupRecyclerView()
        observeViewModel()

        binding.fabAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NewPlaylistFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnCreateFirst.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NewPlaylistFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerView() {
        playlistAdapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                val targetActivity = if (playlist.type == "XSTREAM") {
                    ContainerXstreamActivity::class.java
                } else {
                    ContainerPlaylistActivity::class.java
                }

                val intent = Intent(requireContext(), targetActivity).apply {
                    putExtra("playlistId", playlist.id)
                    putExtra("playlistName", playlist.name)
                    putExtra("playlistType", playlist.type)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            },
            onMoreClick = { playlist, anchorView ->
                showPopupDelete(anchorView, playlist)
            }
        )
        binding.recyclerPlaylists.apply {
            adapter = playlistAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showPopupDelete(anchorView: View, playlist: Playlist) {
        val popupView = layoutInflater.inflate(R.layout.item_delete, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // BẮT BUỘC: Giúp PopupWindow trong suốt để hiển thị được bóng đổ (shadow) của CardView
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        popupView.setOnClickListener {
            popupWindow.dismiss()
            showDeleteDialog(playlist)
        }

        popupWindow.setOnDismissListener {
            anchorView.visibility = View.VISIBLE
        }

        anchorView.visibility = View.INVISIBLE

        // Đo đạc kích thước của nút Delete trước khi vẽ lên màn hình
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        // Tính toán để căn phải và căn giữa theo chiều dọc so với anchorView (nút 3 chấm)
        val xOffset = anchorView.width - popupWidth
        val yOffset = -(anchorView.height + popupHeight) / 2

        // Hiển thị đè lên vị trí của anchorView
        popupWindow.showAsDropDown(anchorView, xOffset, yOffset, Gravity.START)
    }

    private fun showDeleteDialog(playlist: Playlist) {
        // Nạp layout dialog_remove_playlist.xml
        val dialogView = layoutInflater.inflate(R.layout.dialog_remove_playlist, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Ánh xạ nút
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnRemove = dialogView.findViewById<MaterialButton>(R.id.btnRemove)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnRemove.setOnClickListener {
            dialog.dismiss()
            viewModel.deletePlaylist(playlist.id)
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlists.collect { playlists ->
                        if (playlists.isEmpty()) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.recyclerPlaylists.visibility = View.GONE
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.recyclerPlaylists.visibility = View.VISIBLE
                            Log.d("DEBUG", "collect playlists size = ${playlists.size}")
                            playlistAdapter.submitList(playlists)
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is PlaylistUiState.Loading -> { }
                            is PlaylistUiState.Success -> {
                                viewModel.resetState()
                                parentFragmentManager.popBackStack()
                            }
                            is PlaylistUiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                            }
                            is PlaylistUiState.Idle -> { }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}