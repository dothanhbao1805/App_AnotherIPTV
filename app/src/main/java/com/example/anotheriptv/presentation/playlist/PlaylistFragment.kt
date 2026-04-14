// presentation/playlist/PlaylistFragment.kt

package com.example.anotheriptv.presentation.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.anotheriptv.R
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.anotheriptv.databinding.FragmentPlaylistBinding

class PlaylistFragment : Fragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

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

        showEmptyState()

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

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerPlaylists.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}