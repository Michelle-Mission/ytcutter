package app.ytcutter.core.data

import app.ytcutter.core.data.PoToken
import org.junit.Assert.assertEquals
import org.junit.Test

class PoTokenArgsTest {

    @Test
    fun buildsQualifiedExtractorArgs() {
        val tokens = PoToken.Tokens(visitorData = "VD", streaming = "STREAM", player = "PLAY")
        assertEquals(
            "youtube:player_client=default,-tv_downgraded;po_token=web.gvs+STREAM,web.player+PLAY;visitor_data=VD",
            PoToken.buildExtractorArgs(tokens)
        )
    }
}
