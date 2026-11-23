package app.ytcutter.core.data

import app.ytcutter.core.model.Action
import app.ytcutter.core.data.ErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun botChallengeAsksForCookies() {
        val error = ErrorMapper.map("ERROR: [youtube] dQw4w9WgXcQ: Sign in to confirm you're not a bot")
        assertEquals(Action.IMPORT_COOKIES, error.action)
        assertTrue(error.message.contains("cookies", ignoreCase = true))
    }

    @Test
    fun rateLimitAsksForCookies() {
        val error = ErrorMapper.map("ERROR: Unable to download webpage: HTTP Error 429: Too Many Requests")
        assertEquals(Action.IMPORT_COOKIES, error.action)
    }

    @Test
    fun ageRestrictedAsksForCookies() {
        val error = ErrorMapper.map("ERROR: This video is age-restricted")
        assertEquals(Action.IMPORT_COOKIES, error.action)
    }

    @Test
    fun unsupportedUrlHasNoAction() {
        val error = ErrorMapper.map("ERROR: Unsupported URL: https://example.com/x")
        assertEquals(Action.NONE, error.action)
    }

    @Test
    fun formatProblemSuggestsRetry() {
        val error = ErrorMapper.map("ERROR: Requested format is not available")
        assertEquals(Action.RETRY, error.action)
    }

    @Test
    fun unknownErrorIsPassedThroughAndShortened() {
        val raw = "ERROR: sesuatu yang aneh " + "x".repeat(400)
        val error = ErrorMapper.map(raw)
        assertEquals(Action.NONE, error.action)
        assertTrue(error.message.length <= 220)
    }

    @Test
    fun blankFallsBackToGenericMessage() {
        val error = ErrorMapper.map("")
        assertEquals(Action.NONE, error.action)
        assertTrue(error.message.isNotBlank())
    }
}