package com.example.anotheriptv.data.remote.parser

import com.example.anotheriptv.data.local.entity.ChannelEntity

class M3UParser {

    fun parse(content: String, playlistId: Long): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("#EXTINF")) {
                val name = Regex("""tvg-name="([^"]*)"""")
                    .find(line)?.groupValues?.get(1)
                    ?: line.substringAfterLast(",").trim()

                val logo = Regex("""tvg-logo="([^"]*)"""")
                    .find(line)?.groupValues?.get(1) ?: ""

                val category = Regex("""group-title="([^"]*)"""")
                    .find(line)?.groupValues?.get(1) ?: "Uncategorized"

                val url = lines.getOrNull(i + 1)?.trim() ?: ""

                if (url.isNotEmpty() && !url.startsWith("#")) {
                    channels.add(
                        ChannelEntity(
                            playlistId  = playlistId,
                            contentType = "LIVE",
                            name        = name,
                            url         = url,
                            category    = category,
                            logo        = logo
                        )
                    )
                }
                i += 2
            } else {
                i++
            }
        }

        return channels
    }

}