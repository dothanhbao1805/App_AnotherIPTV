package com.example.anotheriptv.presentation.xstream.movie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ActivityDetailMovieBinding
import com.example.anotheriptv.presentation.player.PlayerActivity

class DetailMovieActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailMovieBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val logo        = intent.getStringExtra(EXTRA_LOGO).orEmpty()
        val name        = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val rating      = intent.getFloatExtra(EXTRA_RATING, 0f)
        val releaseDate = intent.getStringExtra(EXTRA_RELEASE_DATE).orEmpty()
        val format      = intent.getStringExtra(EXTRA_FORMAT).orEmpty()
        val streamUrl   = intent.getStringExtra(EXTRA_STREAM_URL).orEmpty()

        bindPoster(logo)
        bindTitle(name)
        bindRating(rating)
        bindFormat(format)
        bindReleaseDate(releaseDate)
        bindTrailer(name)
        bindStartWatching(name, streamUrl)
        bindBackButton()
    }

    // ── Poster + blurred background ───────────────────────────────────────────

    private fun bindPoster(logo: String) {
        if (logo.isBlank()) return

        Glide.with(this)
            .load(logo)
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .into(binding.ivMoviePoster)

        Glide.with(this)
            .load(logo)
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .into(binding.ivBackgroundBlur)
    }

    // ── Title ─────────────────────────────────────────────────────────────────

    private fun bindTitle(name: String) {
        binding.tvMovieTitle.text = name
    }

    // ── Rating — ẩn nếu không có ─────────────────────────────────────────────

    private fun bindRating(rating: Float) {
        if (rating > 0f) {
            binding.layoutRating.visibility = View.VISIBLE
            binding.tvRating.text = String.format("%.1f/10", rating)
        } else {
            binding.layoutRating.visibility = View.GONE
        }
    }

    // ── Format badge — luôn hiển thị, fallback "N/A" ─────────────────────────

    private fun bindFormat(format: String) {
        binding.tvFormat.text = format.uppercase().ifBlank { "MP4" }
    }

    // ── Added date — luôn hiển thị, fallback "—" ─────────────────────────────

    private fun bindReleaseDate(releaseDate: String) {
        binding.tvAddedDateValue.text = releaseDate.ifBlank { "April 21, 2026" }
    }

    // ── Trailer — tìm kiếm YouTube theo tên phim ─────────────────────────────

    private fun bindTrailer(movieName: String) {
        binding.btnTrailer.setOnClickListener {
            val query = Uri.encode("$movieName trailer")

            // Thử mở app YouTube trước, fallback sang trình duyệt
            val youtubeAppIntent = Intent(
                Intent.ACTION_SEARCH,
                Uri.parse("vnd.youtube:")
            ).apply {
                putExtra("query", "$movieName trailer")
                setPackage("com.google.android.youtube")
            }

            val youtubeWebIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=$query")
            )

            if (youtubeAppIntent.resolveActivity(packageManager) != null) {
                startActivity(youtubeAppIntent)
            } else {
                startActivity(youtubeWebIntent)
            }
        }
    }

    // ── Start Watching ────────────────────────────────────────────────────────

    private fun bindStartWatching(channelName: String, streamUrl: String) {
        binding.btnStartWatching.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("channelName", channelName)
                putExtra("streamUrl", streamUrl)
            }
            startActivity(intent)
        }
    }

    // ── Back ──────────────────────────────────────────────────────────────────

    private fun bindBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        const val EXTRA_LOGO         = "extra_logo"
        const val EXTRA_NAME         = "extra_name"
        const val EXTRA_RATING       = "extra_rating"
        const val EXTRA_RELEASE_DATE = "extra_release_date"
        const val EXTRA_FORMAT       = "extra_format"
        const val EXTRA_STREAM_URL   = "extra_stream_url"
    }
}