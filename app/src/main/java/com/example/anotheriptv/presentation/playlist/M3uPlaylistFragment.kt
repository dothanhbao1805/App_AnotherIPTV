package com.example.anotheriptv.presentation.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentM3uPlaylistBinding
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.presentation.ContainerPlaylistActivity
import com.example.anotheriptv.presentation.playlist.UiState.PlaylistUiState
import com.example.anotheriptv.presentation.playlist.ViewModel.PlaylistViewModel
import com.example.anotheriptv.presentation.playlist.ViewModelFactory.PlaylistViewModelFactory
import kotlinx.coroutines.launch

class M3uPlaylistFragment : Fragment() {

    private var _binding: FragmentM3uPlaylistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlaylistViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        PlaylistViewModelFactory(
            container.getPlaylistsUseCase,
            container.addPlaylistUseCase,
            container.deletePlaylistUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentM3uPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        selectTab(isUrl = true)
        observeUiState()

        binding.tabUrl.setOnClickListener { selectTab(isUrl = true) }
        binding.tabFile.setOnClickListener { selectTab(isUrl = false) }

        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {

                binding.layoutError.visibility = View.GONE
                updateButtonState()
            }
        }

        binding.etPlaylistName.addTextChangedListener(textWatcher)
        binding.etUrl.addTextChangedListener(textWatcher)

        binding.btnCreatePlaylist.setOnClickListener {
            handleCreateAction()
        }
    }

    private fun handleCreateAction() {
        val name = binding.etPlaylistName.text.toString().trim()
        val url = binding.etUrl.text.toString().trim()

        // Kiểm tra định dạng nhanh tại Fragment
        if (name.isEmpty() || url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            binding.layoutError.visibility = View.VISIBLE
            return
        }

        val playlist = Playlist(
            name = name,
            type = "M3U",
            sourceType = "URL",
            m3uUrl = url,
            createdAt = System.currentTimeMillis()
        )
        viewModel.addPlaylist(playlist)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PlaylistUiState.Loading -> {
                            binding.btnCreatePlaylist.isEnabled = false
                        }
                        is PlaylistUiState.Success -> {
                            state.playlist?.let { newPlaylist ->
                                val intent = android.content.Intent(requireContext(), ContainerPlaylistActivity::class.java).apply {
                                    putExtra("playlistId", newPlaylist.id)
                                    putExtra("playlistName", newPlaylist.name)
                                }
                                startActivity(intent)

                                viewModel.resetState()
                                parentFragmentManager.popBackStack()
                            }
                        }
                        is PlaylistUiState.Error -> {
                            binding.layoutError.visibility = View.VISIBLE
                            binding.btnCreatePlaylist.isEnabled = true
                            viewModel.resetState()
                        }
                        is PlaylistUiState.Idle -> {
                            updateButtonState()
                        }
                    }
                }
            }
        }
    }

    private fun selectTab(isUrl: Boolean) {
        val colorSelected = ContextCompat.getColor(requireContext(), R.color.white)
        val colorUnselected = ContextCompat.getColor(requireContext(), R.color.tab_unselected)

        binding.tabUrl.setBackgroundResource(if (isUrl) R.drawable.bg_tab_selected else android.R.color.transparent)
        binding.tabFile.setBackgroundResource(if (isUrl) android.R.color.transparent else R.drawable.bg_tab_selected)

        binding.ivTabUrlIcon.setColorFilter(if (isUrl) colorSelected else colorUnselected)
        binding.ivTabFileIcon.setColorFilter(if (isUrl) colorUnselected else colorSelected)
        binding.tvTabUrl.setTextColor(if (isUrl) colorSelected else colorUnselected)
        binding.tvTabFile.setTextColor(if (isUrl) colorUnselected else colorSelected)

        binding.layoutUrlInput.visibility = if (isUrl) View.VISIBLE else View.GONE
        binding.layoutFileInput.visibility = if (isUrl) View.GONE else View.VISIBLE
        binding.labelSource.text = if (isUrl) "M3U URL" else "M3U File"
    }

    private fun updateButtonState() {
        val url = binding.etUrl.text.toString().trim()
        val isEnabled = url.isNotEmpty()

        binding.btnCreatePlaylist.isEnabled = isEnabled
        binding.btnCreatePlaylist.setBackgroundResource(
            if (isEnabled) R.drawable.bg_button_primary else R.drawable.bg_button_disabled
        )

        val contentColor = ContextCompat.getColor(requireContext(), if (isEnabled) R.color.white else R.color.tab_unselected)
        binding.tvCreateBtn.setTextColor(contentColor)
        binding.btnCreatePlaylist.findViewById<android.widget.ImageView>(R.id.ivSaveIcon)?.setColorFilter(contentColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}