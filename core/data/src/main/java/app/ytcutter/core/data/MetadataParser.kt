package app.ytcutter.core.data

import org.json.JSONObject

object MetadataParser {

    fun parse(json: String): VideoInfo? {
        if (json.isBlank()) return null
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            return null
        }
        val id = root.optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = root.optString("title").takeIf { it.isNotBlank() } ?: return null
        val duration = root.optInt("duration", 0)
        val thumbnail = root.optString("thumbnail").takeIf { it.isNotBlank() }

        val heights = sortedSetOf<Int>()
        var hasAudio = false
        val formats = root.optJSONArray("formats")
        if (formats != null) {
            for (i in 0 until formats.length()) {
                val f = formats.optJSONObject(i) ?: continue
                val vcodec = f.optString("vcodec", "none")
                val acodec = f.optString("acodec", "none")
                if (acodec != "none") hasAudio = true
                if (vcodec != "none" && f.has("height")) {
                    heights.add(f.optInt("height"))
                }
            }
        }

        return VideoInfo(
            id = id,
            title = title,
            durationSec = duration,
            thumbnailUrl = thumbnail,
            heights = heights,
            hasAudio = hasAudio
        )
    }
}