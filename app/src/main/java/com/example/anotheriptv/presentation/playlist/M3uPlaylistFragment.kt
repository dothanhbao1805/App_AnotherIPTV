package com.example.anotheriptv.presentation.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentM3uPlaylistBinding
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.presentation.playlist.ViewModel.PlaylistViewModel
import com.example.anotheriptv.presentation.playlist.ViewModelFactory.PlaylistViewModelFactory

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

        // Mặc định chọn URL
        selectTab(isUrl = true)

        binding.tabUrl.setOnClickListener { selectTab(isUrl = true) }
        binding.tabFile.setOnClickListener { selectTab(isUrl = false) }

        binding.layoutFileInput.setOnClickListener {
            // TODO: mở file picker
        }

        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateButtonState()
            }
        }
        binding.etPlaylistName.addTextChangedListener(textWatcher)
        binding.etUrl.addTextChangedListener(textWatcher)

        binding.btnCreatePlaylist.setOnClickListener {
            val name = binding.etPlaylistName.text.toString().trim()
            val url = binding.etUrl.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên playlist", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (url.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(requireContext(), "URL phải bắt đầu bằng http:// hoặc https://", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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
    }

    private fun selectTab(isUrl: Boolean) {
        val colorSelected = ContextCompat.getColor(requireContext(), R.color.white)
        val colorUnselected = ContextCompat.getColor(requireContext(), R.color.tab_unselected)

        // Background tabs
        binding.tabUrl.setBackgroundResource(
            if (isUrl) R.drawable.bg_tab_selected else android.R.color.transparent
        )
        binding.tabFile.setBackgroundResource(
            if (isUrl) android.R.color.transparent else R.drawable.bg_tab_selected
        )

        // Icon tint
        binding.ivTabUrlIcon.setColorFilter(if (isUrl) colorSelected else colorUnselected)
        binding.ivTabFileIcon.setColorFilter(if (isUrl) colorUnselected else colorSelected)

        // Text color
        binding.tvTabUrl.setTextColor(if (isUrl) colorSelected else colorUnselected)
        binding.tvTabFile.setTextColor(if (isUrl) colorUnselected else colorSelected)

        // Show/hide input
        binding.layoutUrlInput.visibility = if (isUrl) View.VISIBLE else View.GONE
        binding.layoutFileInput.visibility = if (isUrl) View.GONE else View.VISIBLE
        binding.labelSource.text = if (isUrl) "M3U URL" else "M3U File"
    }

    private fun updateButtonState() {
        val name = binding.etPlaylistName.text.toString().trim()
        val url = binding.etUrl.text.toString().trim()
        val isEnabled = name.isNotEmpty() && url.isNotEmpty()

        binding.btnCreatePlaylist.isEnabled = isEnabled
        binding.btnCreatePlaylist.setBackgroundResource(
            if (isEnabled) R.drawable.bg_button_primary else R.drawable.bg_button_disabled
        )

        // Đổi màu icon và text theo trạng thái
        val textColor = if (isEnabled) {
            ContextCompat.getColor(requireContext(), R.color.white)
        } else {
            ContextCompat.getColor(requireContext(), R.color.tab_unselected)
        }
        binding.tvCreateBtn.setTextColor(textColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}