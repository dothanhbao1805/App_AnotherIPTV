package com.example.anotheriptv.data.remote.parser

import com.example.anotheriptv.data.local.entity.ChannelEntity
import org.json.JSONArray

class XstreamParser {

    fun parseLive(
        json: String, playlistId: Long,
        baseUrl: String, username: String, password: String
    ): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val array = tryParseArray(json) ?: return channels

        for (i in 0 until array.length()) {
            val obj      = array.optJSONObject(i) ?: continue
            val streamId = obj.optString("stream_id", "")
            val url      = obj.optString("direct_source", "")
                .ifBlank { "$baseUrl/live/$username/$password/$streamId.ts" }

            channels.add(ChannelEntity(
                playlistId  = playlistId,
                contentType = "LIVE",
                name        = obj.optString("name", "Unknown"),
                url         = url,
                logo        = obj.optString("stream_icon", ""),
                category    = obj.optString("category_name", "Uncategorized")
            ))
        }
        return channels
    }

    fun parseMovies(
        json: String, playlistId: Long,
        baseUrl: String, username: String, password: String
    ): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val array = tryParseArray(json) ?: return channels

        for (i in 0 until array.length()) {
            val obj      = array.optJSONObject(i) ?: continue
            val streamId = obj.optString("stream_id", "")
            val ext      = obj.optString("container_extension", "mp4")
            val url      = obj.optString("direct_source", "")
                .ifBlank { "$baseUrl/movie/$username/$password/$streamId.$ext" }

            channels.add(ChannelEntity(
                playlistId  = playlistId,
                contentType = "MOVIE",
                name        = obj.optString("name", "Unknown"),
                url         = url,
                logo        = obj.optString("stream_icon", ""),
                category    = obj.optString("category_name", "Uncategorized"),
                rating      = obj.optString("rating", "").toFloatOrNull(),
                releaseDate = obj.optString("releaseDate", "").ifBlank { null },
                genre       = obj.optString("genre", "").ifBlank { null },
                cast        = obj.optString("cast", "").ifBlank { null },
                description = obj.optString("plot", "").ifBlank { null },
                trailerUrl  = obj.optString("youtube_trailer", "").ifBlank { null }
                    ?.let { "https://www.youtube.com/watch?v=$it" }
            ))
        }
        return channels
    }

    fun parseSeries(
        json: String, playlistId: Long,
        baseUrl: String, username: String, password: String
    ): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val array = tryParseArray(json) ?: return channels

        for (i in 0 until array.length()) {
            val obj      = array.optJSONObject(i) ?: continue
            val seriesId = obj.optLong("series_id", 0L)

            channels.add(ChannelEntity(
                playlistId  = playlistId,
                contentType = "SERIES",
                name        = obj.optString("name", "Unknown"),
                url         = "",   // episode URL build khi user chọn xem
                logo        = obj.optString("cover", ""),
                category    = obj.optString("category_name", "Uncategorized"),
                rating      = obj.optString("rating", "").toFloatOrNull(),
                releaseDate = obj.optString("releaseDate", "").ifBlank { null },
                genre       = obj.optString("genre", "").ifBlank { null },
                cast        = obj.optString("cast", "").ifBlank { null },
                description = obj.optString("plot", "").ifBlank { null },
                seriesId    = seriesId
            ))
        }
        return channels
    }

    fun parseEpisodes(
        json: String, playlistId: Long, seriesName: String,
        baseUrl: String, username: String, password: String
    ): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val root     = tryParseObject(json) ?: return channels
        val episodes = root.optJSONObject("episodes") ?: return channels

        val seasonKeys = episodes.keys()
        while (seasonKeys.hasNext()) {
            val seasonKey    = seasonKeys.next()
            val seasonNumber = seasonKey.toIntOrNull() ?: continue
            val episodeArray = episodes.optJSONArray(seasonKey) ?: continue

            for (i in 0 until episodeArray.length()) {
                val ep  = episodeArray.optJSONObject(i) ?: continue
                val ext = ep.optString("container_extension", "mp4")
                val id  = ep.optString("id", "")
                val url = ep.optString("direct_source", "")
                    .ifBlank { "$baseUrl/series/$username/$password/$id.$ext" }

                channels.add(ChannelEntity(
                    playlistId      = playlistId,
                    contentType     = "SERIES",
                    name            = ep.optString("title", "$seriesName S${seasonNumber}E${i + 1}"),
                    url             = url,
                    logo            = ep.optString("movie_image", ""),
                    category        = "Series",
                    description     = ep.optString("plot", "").ifBlank { null },
                    episodeDuration = ep.optString("duration_secs", "").toIntOrNull()?.let { it / 60 },
                    seasonNumber    = seasonNumber,
                    episodeNumber   = ep.optString("episode_num", "").toIntOrNull()
                ))
            }
        }
        return channels
    }

    private fun tryParseArray(json: String)  = runCatching { JSONArray(json) }.getOrNull()
    private fun tryParseObject(json: String) = runCatching { org.json.JSONObject(json) }.getOrNull()
}