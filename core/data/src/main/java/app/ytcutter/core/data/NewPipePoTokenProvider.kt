package app.ytcutter.core.data

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor

class NewPipePoTokenProvider(private val context: Context) : PoTokenProvider {

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        val tokens = runBlocking {
            PoToken.tokens(context, "https://www.youtube.com/watch?v=$videoId")
        } ?: return null
        return PoTokenResult(tokens.visitorData, tokens.player, tokens.streaming)
    }
}

fun setNewPipePoTokenProvider(provider: PoTokenProvider) {
    YoutubeStreamExtractor.setPoTokenProvider(provider)
}
