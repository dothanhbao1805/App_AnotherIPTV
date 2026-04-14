package com.example.anotheriptv.presentation.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentM3uPlaylistBinding

class M3uPlaylistFragment : Fragment() {

    private var _binding: FragmentM3uPlaylistBinding? = null
    private val binding get() = _binding!!

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

        binding.btnCreatePlaylist.setOnClickListener {
            // TODO: xử lý tạo playlist
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}